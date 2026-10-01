package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.registry.PathogenTags;
import com.azure.pathogenesis.registry.PathogenTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public final class PathogenSterilization {

    private static final double NOTIFY_RADIUS = 16.0D;

    private PathogenSterilization() {}

    public static boolean sterilize(ServerLevel level, BlockPos pos) {
        if (!Pathogenesis.getConfig().containmentConfigs.fireSterilizesPathogen || !level.isLoaded(pos)) {
            return false;
        }
        var state = level.getBlockState(pos);
        if (!state.is(PathogenTags.Blocks.STERILIZABLE)) {
            return false;
        }
        var result = ContaminationPalette.sterilizedForm(state);
        if (result == null) {
            return false;
        }
        level.setBlock(pos, result, Block.UPDATE_ALL);
        var c = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.5D, c.z, 4, 0.3D, 0.2D, 0.3D, 0.01D);
        if (level.random.nextInt(4) == 0) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
        }
        return true;
    }

    public static void onFire(ServerLevel level, BlockPos firePos) {
        if (!Pathogenesis.getConfig().containmentConfigs.fireSterilizesPathogen) {
            return;
        }
        var sterilized = 0;
        for (var direction : Direction.values()) {
            if (sterilize(level, firePos.relative(direction))) {
                sterilized++;
            }
        }
        if (sterilized > 0) {
            PathogenTriggers.triggerNearby(level, Vec3.atCenterOf(firePos), NOTIFY_RADIUS, PathogenTriggers.STERILIZED);
        }
    }

    public static int sterilizeArea(ServerLevel level, BlockPos center, int radius) {
        var count = 0;
        var rSqr = radius * radius;
        for (
            var pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius)
            )
        ) {
            if (pos.distSqr(center) <= rSqr && sterilize(level, pos.immutable())) {
                count++;
            }
        }
        if (count > 0) {
            PathogenTriggers.triggerNearby(
                level,
                Vec3.atCenterOf(center),
                NOTIFY_RADIUS + radius,
                PathogenTriggers.STERILIZED
            );
        }
        return count;
    }
}
