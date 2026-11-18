package net.edu.cinercia.plugin.util;

import java.util.Set;

public final class PluginProperties {
    public static final String PLATFORM_NAME = "cinercia.platform.name";

    // Project
    public static final String ONLY_CLIENT = "Lnet/edu/cinercia/plugin/annotations/OnlyInClient;";
    public static final String ONLY_SERVER = "Lnet/edu/cinercia/plugin/annotations/OnlyInServer;";
    public static final String DELEGATED = "Lnet/edu/cinercia/plugin/annotations/DelegatedByPlatform;";

    // Fabric
    public static final String CLIENT = "CLIENT";
    public static final String SERVER = "SERVER";
    public static final String FABRIC = "fabric";
    public static final String FABRIC_ENVIRONMENT = "Lnet/fabricmc/api/Environment;";
    public static final String FABRIC_ENV_TYPE = "Lnet/fabricmc/api/EnvType;";

    // NeoForge & Forge
    public static final String DEDICATED_SERVER = "DEDICATED_SERVER";
    public static final String NEOFORGE = "neoforge";
    public static final String FORGE = "forge";
    public static final String NEOFORGE_ONLY_IN = "Lnet/neoforged/api/distmarker/OnlyIn;";
    public static final String NEOFORGE_DIST = "Lnet/neoforged/api/distmarker/Dist;";
    public static final String FORGE_ONLY_IN = "Lnet/minecraftforge/api/distmarker/OnlyIn;";
    public static final String FORGE_DIST = "Lnet/minecraftforge/api/distmarker/Dist;";

    // Porject names that are treated as a platform
    public static final Set<String> PLATFORMS = Set.of(FABRIC, NEOFORGE, FORGE);
}