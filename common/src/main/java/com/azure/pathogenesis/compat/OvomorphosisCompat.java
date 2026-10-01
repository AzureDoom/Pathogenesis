package com.azure.pathogenesis.compat;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.api.PathogenApi;
import com.azure.pathogenesis.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

@SuppressWarnings("unused")
public final class OvomorphosisCompat {

    public static final String OVOMORPHOSIS_ID = "ovomorphosis";

    private static boolean loaded;

    private OvomorphosisCompat() {}

    public static void init() {
        loaded = Services.PLATFORM.isModLoaded(OVOMORPHOSIS_ID);
        if (loaded) {
            Pathogenesis.LOGGER.info("Ovomorphosis detected; alien tags and flame integration active");
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void onFlameImpact(ServerLevel level, BlockPos pos) {
        PathogenApi.sterilizeArea(level, pos, 1);
    }
}
