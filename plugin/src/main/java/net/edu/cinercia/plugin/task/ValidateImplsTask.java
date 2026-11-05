package net.edu.cinercia.plugin.task;

import net.edu.cinercia.plugin.transformer.TransformDelegatedByPlatform;
import net.edu.cinercia.plugin.util.PluginMessages;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.edu.cinercia.plugin.util.PluginProperties.DELEGATED;

// Checks that each @DelegatedByPlatform method has its Impl
public abstract class ValidateImplsTask extends DefaultTask {

    @Internal
    public abstract DirectoryProperty getSourceDir();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    public FileTree getSourceFiles() {
        return getSourceDir().getAsFileTree();
    }

    @Input
    public abstract Property<String> getPlatform();

    @TaskAction
    public void run() throws IOException {
        Path src = getSourceDir().get().getAsFile().toPath();
        String platform = getPlatform().get();
        List<String> problems = new ArrayList<>();

        try (Stream<Path> files = Files.walk(src)) {
            for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".class"))::iterator) {
                byte[] bytes = Files.readAllBytes(file);
                if (!new String(bytes, StandardCharsets.ISO_8859_1).contains(DELEGATED)) {
                    continue;
                }
                ClassNode owner = read(bytes);
                for (MethodNode method : owner.methods) {
                    if (TransformDelegatedByPlatform.hasMarker(method)) {
                        check(src, platform, owner, method, problems);
                    }
                }
            }
        }

        if (!problems.isEmpty()) {
            throw new GradleException(PluginMessages.invalidImpls(platform,
                    problems.stream().map(p -> "  - " + p).collect(Collectors.joining("\n"))));
        }
    }

    private static void check(Path src, String platform, ClassNode owner, MethodNode method, List<String> problems) throws IOException {
        String implName = TransformDelegatedByPlatform.getPlatformClass(platform, owner.name);
        String impl = implName.replace('/', '.');
        String ownerName = owner.name.replace('/', '.');
        String signature = signature(method);
        Path file = src.resolve(implName + ".class");

        if (!Files.exists(file)) {
            problems.add(PluginMessages.missingClass(impl, ownerName, method.name, signature));
            return;
        }

        MethodNode found = read(Files.readAllBytes(file)).methods.stream()
                .filter(m -> m.name.equals(method.name) && m.desc.equals(method.desc))
                .findFirst().orElse(null);

        if (found == null) {
            problems.add(PluginMessages.missingMethod(impl, ownerName, method.name, signature));
        } else if ((found.access & Opcodes.ACC_STATIC) == 0) {
            problems.add(PluginMessages.implNotStatic(impl, method.name));
        }
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE);
        return node;
    }

    private static String signature(MethodNode method) {
        Type type = Type.getMethodType(method.desc);
        String args = Arrays.stream(type.getArgumentTypes())
                .map(Type::getClassName).collect(Collectors.joining(", "));
        return type.getReturnType().getClassName() + " " + method.name + "(" + args + ")";
    }
}