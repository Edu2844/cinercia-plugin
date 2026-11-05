/*
 * Portions of this file are adapted from architectury-transformer
 * (dev.architectury.transformer.transformers.TransformExpectPlatform),
 * licensed under the MIT License.
 *
 * Copyright (c) 2020, 2021, 2022 architectury.
 * See THIRD_PARTY_NOTICES.md for the full license text and source link.
 */
package net.edu.cinercia.plugin.transformer;

import net.edu.cinercia.plugin.util.PluginMessages;
import net.edu.cinercia.plugin.util.PluginProperties;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Map;
import java.util.Set;

import static net.edu.cinercia.plugin.util.PluginProperties.DELEGATED;

// Redirects @DelegatedByPlatform methods to their platform Impl
public class TransformDelegatedByPlatform implements ClassEditTransformer {

    private static final Logger LOGGER = Logging.getLogger(TransformDelegatedByPlatform.class);

    private String platform;

    @Override
    public Set<String> annotations() {
        return Set.of(DELEGATED);
    }

    @Override
    public void supplyProperties(Map<String, String> properties) {
        platform = properties.get(PluginProperties.PLATFORM_NAME);
    }

    @Override
    public ClassNode doEdit(String name, ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!hasMarker(method)) {
                continue;
            }
            if ((method.access & Opcodes.ACC_STATIC) == 0) {
                throw new GradleException(PluginMessages.notStatic(node.name, method.name));
            }

            removeMarker(method);
            method.instructions.clear();
            method.tryCatchBlocks.clear();
            method.localVariables = null;
            method.visibleLocalVariableAnnotations = null;
            method.invisibleLocalVariableAnnotations = null;

            Type type = Type.getMethodType(method.desc);
            int stackIndex = 0;
            for (Type argumentType : type.getArgumentTypes()) {
                method.instructions.add(new VarInsnNode(argumentType.getOpcode(Opcodes.ILOAD), stackIndex));
                stackIndex += argumentType.getSize();
            }

            String impl = getPlatformClass(platform, node.name);
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, impl, method.name, method.desc, false));
            method.instructions.add(new InsnNode(type.getReturnType().getOpcode(Opcodes.IRETURN)));

            LOGGER.info(PluginMessages.delegated(node.name, method.name, impl));
        }
        return node;
    }

    public static boolean hasMarker(MethodNode method) {
        return has(method.visibleAnnotations) || has(method.invisibleAnnotations);
    }

    private static boolean has(java.util.List<AnnotationNode> annotations) {
        return annotations != null && annotations.stream().anyMatch(it -> DELEGATED.equals(it.desc));
    }

    private static void removeMarker(MethodNode method) {
        if (method.visibleAnnotations != null) {
            method.visibleAnnotations.removeIf(it -> DELEGATED.equals(it.desc));
        }
        if (method.invisibleAnnotations != null) {
            method.invisibleAnnotations.removeIf(it -> DELEGATED.equals(it.desc));
        }
    }

    // From net/edu/Class to net/edu/platform like fabric or neo/ClassImpl
    public static String getPlatformClass(String platform, String ownerClass) {
        String type = ownerClass.replace("$", "") + "Impl";
        int slash = type.lastIndexOf('/');
        String pkg = slash >= 0 ? type.substring(0, slash + 1) : "";
        return pkg + platform + "/" + type.substring(slash + 1);
    }
}