package com.azure.pathogenesis.api;

import com.azure.pathogenesis.contamination.PathogenSterilization;
import com.azure.pathogenesis.contamination.PathogenZone;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.contamination.RuptureStrength;
import com.azure.pathogenesis.exposure.ExposureType;
import com.azure.pathogenesis.exposure.PathogenExposureHelper;
import com.azure.pathogenesis.infection.InfectionSite;
import com.azure.pathogenesis.infection.NeomorphInfections;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public final class PathogenApi {

    private PathogenApi() {}

    public static boolean isContaminated(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(PathogenTags.Blocks.CONTAMINATED);
    }

    public static void sterilizeArea(ServerLevel level, BlockPos center, int radius) {
        PathogenSterilization.sterilizeArea(level, center, radius);
    }

    public static void sterilize(ServerLevel level, BlockPos pos) {
        PathogenSterilization.sterilize(level, pos);
    }

    public static void rupture(ServerLevel level, BlockPos pos) {
        PathogenZoneManager.onRupture(level, pos, RuptureStrength.RUPTURE, false);
    }

    @Nullable
    public static PathogenZone zoneAt(ServerLevel level, BlockPos pos) {
        return PathogenZoneManager.findZone(level, pos);
    }

    public static boolean isInfected(LivingEntity entity) {
        return NeomorphInfections.isInfected(entity);
    }

    public static boolean infect(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !NeomorphInfections.isValidHost(entity)) {
            return false;
        }
        return NeomorphInfections.infect(
            entity,
            new InfectionSite(entity.blockPosition(), PathogenZoneManager.findZoneId(level, entity.blockPosition()))
        );
    }

    public static void expose(LivingEntity entity, int amount) {
        PathogenExposureHelper.expose(entity, amount, ExposureType.DIRECT);
    }
}
