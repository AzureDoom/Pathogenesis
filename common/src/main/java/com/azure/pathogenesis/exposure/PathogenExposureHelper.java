package com.azure.pathogenesis.exposure;

import com.azure.pathogenesis.contamination.PathogenClimate;
import com.azure.pathogenesis.fluid.ContaminatedWaterFluid;
import com.azure.pathogenesis.infection.PathogenHosts;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PathogenExposureHelper {

    private PathogenExposureHelper() {}

    public static boolean canBeExposed(LivingEntity entity) {
        if (!entity.isAlive() || entity.getType().is(PathogenTags.Entities.PATHOGEN_IMMUNE)) {
            return false;
        }
        return !(entity instanceof Player player) || !(player.isCreative() || player.isSpectator());
    }

    public static boolean isWashing(LivingEntity entity) {
        if (!entity.isInWaterOrRain()) {
            return false;
        }
        var level = entity.level();
        return !(level.getFluidState(entity.blockPosition()).getType() instanceof ContaminatedWaterFluid)
            && !(level.getFluidState(BlockPos.containing(entity.getEyePosition()))
                .getType() instanceof ContaminatedWaterFluid);
    }

    public static void expose(LivingEntity entity, int amount, ExposureType type) {
        if (canBeExposed(entity)) {
            PathogenHosts.getOrCreate(entity).getOrCreateExposure().add(amount, type, entity.level().getGameTime());
        }
    }

    public static void contact(LivingEntity entity, ExposureType type) {
        if (!canBeExposed(entity)) {
            return;
        }
        var exposure = PathogenHosts.getOrCreate(entity).getOrCreateExposure();
        var now = entity.level().getGameTime();
        if (exposure.tryContact(now)) {
            exposure.add(1, type, now);
        }
    }

    public static void exposeArea(ServerLevel level, Vec3 center, double radius, int amount, ExposureType type) {
        if (type == ExposureType.DIRECT || type == ExposureType.SPORE) {
            amount = (int) Math.round(amount * PathogenClimate.airborneMultiplier(level, BlockPos.containing(center)));
            if (amount <= 0) {
                return;
            }
        }
        var radiusSqr = radius * radius;
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius))) {
            var distanceSqr = entity.distanceToSqr(center);
            if (distanceSqr <= radiusSqr) {
                var falloff = 1.0D - 0.5D * Math.sqrt(distanceSqr) / radius;
                expose(entity, Math.max(1, (int) Math.round(amount * falloff)), type);
            }
        }
    }
}
