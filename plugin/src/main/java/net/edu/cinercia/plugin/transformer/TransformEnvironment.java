package net.edu.cinercia.plugin.transformer;

import net.edu.cinercia.plugin.util.PluginMessages;
import net.edu.cinercia.plugin.util.PluginProperties;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.edu.cinercia.plugin.util.PluginProperties.*;

// Replaces @OnlyInClient and @OnlyInServer with the platform annotation
public class TransformEnvironment implements ClassEditTransformer {
    private static final Logger LOGGER = Logging.getLogger(TransformEnvironment.class);

    private String annotation;
    private String enumType;
    private String serverValue;

    @Override
    public Set<String> annotations() {
        return Set.of(ONLY_CLIENT, ONLY_SERVER);
    }

    @Override
    public void supplyProperties(Map<String, String> properties) {
        String platform = properties.get(PluginProperties.PLATFORM_NAME);
        switch (platform) {
            case FABRIC -> remap(FABRIC_ENVIRONMENT, FABRIC_ENV_TYPE, SERVER);
            case NEOFORGE -> remap(NEOFORGE_ONLY_IN, NEOFORGE_DIST, DEDICATED_SERVER);
            case FORGE -> remap(FORGE_ONLY_IN, FORGE_DIST, DEDICATED_SERVER);
            default -> throw new GradleException(PluginMessages.unknownPlatform(platform));
        }
    }

    private void remap(String annotation, String enumType, String serverValue) {
        this.annotation = annotation;
        this.enumType = enumType;
        this.serverValue = serverValue;
    }

    @Override
    public ClassNode doEdit(String name, ClassNode node) {
        editAnnotations(node.visibleAnnotations, node.name);
        for (FieldNode field : node.fields) {
            editAnnotations(field.visibleAnnotations, node.name + "." + field.name);
        }
        for (MethodNode method : node.methods) {
            editAnnotations(method.visibleAnnotations, node.name + "." + method.name);
        }
        return node;
    }

    private void editAnnotations(List<AnnotationNode> annotations, String where) {
        if (annotations == null) {
            return;
        }
        for (AnnotationNode node : annotations) {
            boolean client = ONLY_CLIENT.equals(node.desc);
            if (!client && !ONLY_SERVER.equals(node.desc)) {
                continue;
            }

            String value = client ? CLIENT : serverValue;
            node.desc = annotation;
            node.values = new ArrayList<>(List.of("value", new String[]{enumType, value}));
            LOGGER.info(PluginMessages.environment(where, annotation.substring(annotation.lastIndexOf('/') + 1, annotation.length() - 1), value));
        }
    }
}