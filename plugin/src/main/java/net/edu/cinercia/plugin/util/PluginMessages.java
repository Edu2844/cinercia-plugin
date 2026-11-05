package net.edu.cinercia.plugin.util;

public final class PluginMessages {
    public static final String PREFIX = "[CinerciaPlugin] ";

    // Logs
    public static String transformed(String task, int count, String platform) {
        return PREFIX + task + ": " + count + " class(es) transformed for " + platform;
    }

    public static String environment(String where, String annotation, String value) {
        return PREFIX + where + " -> @" + annotation + "(" + value + ")";
    }

    public static String delegated(String owner, String method, String impl) {
        return PREFIX + owner + "." + method + " -> " + impl;
    }

    // Errors
    public static String unknownPlatform(String platform) {
        return PREFIX + "Unknown platform '" + platform + "'. Use fabric, neoforge or forge.";
    }

    public static String notStatic(String owner, String method) {
        return PREFIX + "@DelegatedByPlatform can only apply to static methods: " + owner + "." + method;
    }

    public static String processing(String file, String reason) {
        return PREFIX + "Failed to process " + file + ": " + reason;
    }

    public static String missingClass(String impl, String owner, String method, String signature) {
        return impl + " does not exist (needed by " + owner + "." + method + "). Add: public static " + signature;
    }

    public static String missingMethod(String impl, String owner, String method, String signature) {
        return impl + " has no matching method (needed by " + owner + "." + method + "). Add: public static " + signature;
    }

    public static String implNotStatic(String impl, String method) {
        return impl + "." + method + " must be static.";
    }

    public static String invalidImpls(String platform, String problems) {
        return PREFIX + "Invalid platform implementations for " + platform + ":\n" + problems;
    }
}