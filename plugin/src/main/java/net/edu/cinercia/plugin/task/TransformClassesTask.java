package net.edu.cinercia.plugin.task;

import net.edu.cinercia.plugin.transformer.ClassEditTransformer;
import net.edu.cinercia.plugin.transformer.TransformDelegatedByPlatform;
import net.edu.cinercia.plugin.transformer.TransformEnvironment;
import net.edu.cinercia.plugin.util.PluginMessages;
import net.edu.cinercia.plugin.util.PluginProperties;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

// Copies the compiled classes and transforms the annotated ones
@CacheableTask
public abstract class TransformClassesTask extends DefaultTask {

    @Internal
    public abstract DirectoryProperty getSourceDir();

    // Skips the task if there are no classes
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    public FileTree getSourceFiles() {
        return getSourceDir().getAsFileTree();
    }

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getPlatform();

    @TaskAction
    public void run() throws IOException {
        Path src = getSourceDir().get().getAsFile().toPath();
        Path out = getOutputDir().get().getAsFile().toPath();
        String platform = getPlatform().get();
        clean(out);

        List<ClassEditTransformer> transformers = List.of(
                new TransformEnvironment(), new TransformDelegatedByPlatform());
        Map<String, String> properties = Map.of(PluginProperties.PLATFORM_NAME, platform);
        transformers.forEach(t -> t.supplyProperties(properties));

        int transformed = 0;
        try (Stream<Path> files = Files.walk(src)) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                Path target = out.resolve(src.relativize(file).toString());
                Files.createDirectories(target.getParent());

                byte[] bytes = Files.readAllBytes(file);
                if (file.toString().endsWith(".class")) {
                    byte[] result = transform(file, bytes, transformers);
                    if (result != bytes) {
                        transformed++;
                    }
                    bytes = result;
                }
                Files.write(target, bytes);
            }
        }

        if (transformed > 0) {
            getLogger().lifecycle(PluginMessages.transformed(getPath(), transformed, platform));
        }
    }

    private byte[] transform(Path file, byte[] bytes, List<ClassEditTransformer> transformers) {
        // Skip classes without plugin annotations
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);
        List<ClassEditTransformer> active = transformers.stream()
                .filter(t -> t.annotations().stream().anyMatch(raw::contains))
                .toList();
        if (active.isEmpty()) {
            return bytes;
        }

        try {
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            for (ClassEditTransformer transformer : active) {
                node = transformer.doEdit(node.name, node);
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        } catch (GradleException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new GradleException(
                    PluginMessages.processing(file.getFileName().toString(), e.getMessage()), e);
        }
    }

    private static void clean(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                if (!path.equals(dir)) {
                    Files.delete(path);
                }
            }
        }
    }
}