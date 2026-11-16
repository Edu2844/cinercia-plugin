package net.edu.cinercia.plugin.transformer;

import org.objectweb.asm.tree.ClassNode;

import java.util.Map;
import java.util.Set;

// Annotations this transformer looks for
public interface ClassEditTransformer {

    Set<String> annotations();

    default void supplyProperties(Map<String, String> properties) {}

    ClassNode doEdit(String name, ClassNode node);
}