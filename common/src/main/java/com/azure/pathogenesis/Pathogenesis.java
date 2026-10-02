package com.azure.pathogenesis;

import com.azure.pathogenesis.compat.OvomorphosisCompat;
import com.azure.pathogenesis.config.PathogenesisConfig;
import com.azure.pathogenesis.registry.*;
import mod.azure.azurelib.AzureLibMod;
import mod.azure.azurelib.common.config.format.ConfigFormats;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Pathogenesis {

    public static final String MOD_ID = "pathogenesis";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static PathogenesisConfig config;

    private Pathogenesis() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init() {
        config = AzureLibMod.registerConfig(PathogenesisConfig.class, ConfigFormats.json()).getConfigInstance();
        PathogenSounds.init();
        PathogenFluids.init();
        PathogenBlocks.init();
        PathogenBlockEntities.init();
        PathogenEntities.init();
        PathogenItems.init();
        PathogenTriggers.init();
        OvomorphosisCompat.init();
        PathogenStructureTypes.init();
    }

    public static PathogenesisConfig getConfig() {
        return config;
    }
}
