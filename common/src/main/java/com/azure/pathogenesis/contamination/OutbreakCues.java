package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.block.SporePlantBlock;
import com.azure.pathogenesis.entity.SporeCloudEntity;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import com.azure.pathogenesis.registry.PathogenTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

final class OutbreakCues {

    private OutbreakCues() {}

    static void onPhaseChange(ServerLevel level, PathogenZone zone, OutbreakPhase previous) {
        if (zone.phase() == OutbreakPhase.ECOLOGICAL && previous != OutbreakPhase.ECOLOGICAL) {
            becameEcological(level, zone);
        }
        OutbreakSync.pushNearby(level, zone);
    }

    private static void becameEcological(ServerLevel level, PathogenZone zone) {
        var center = Vec3.atCenterOf(zone.origin());
        var reach = zone.radius() + 32.0D;
        var reachSqr = reach * reach;
        for (var player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(center) > reachSqr) {
                continue;
            }
            player.playNotifySound(
                PathogenSounds.OUTBREAK_ECOLOGICAL.get(),
                SoundSource.AMBIENT,
                0.9F,
                0.55F + level.random.nextFloat() * 0.1F
            );
        }
        pulseFlora(level, zone);
        PathogenTriggers.triggerNearby(level, center, reach, PathogenTriggers.ZONE_ECOLOGICAL);
    }

    private static void pulseFlora(ServerLevel level, PathogenZone zone) {
        var origin = zone.origin();
        var random = level.random;
        var pos = new BlockPos.MutableBlockPos();
        var pulses = 0;
        for (var i = 0; i < 64 && pulses < 16; i++) {
            var angle = random.nextDouble() * Math.PI * 2.0D;
            var distance = Math.sqrt(random.nextDouble()) * zone.radius();
            var x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
            var z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            var y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - origin.getY()) > PathogenZone.VERTICAL_REACH) {
                continue;
            }
            pos.set(x, y, z);
            var state = level.getBlockState(pos);
            if (!isFlora(state)) {
                continue;
            }
            if (state.getBlock() instanceof SporePlantBlock) {
                level.blockEvent(pos.immutable(), state.getBlock(), 1, 0);
            }
            level.sendParticles(
                SporeCloudEntity.SPORE_DUST,
                x + 0.5D,
                y + 0.6D,
                z + 0.5D,
                6,
                0.25D,
                0.3D,
                0.25D,
                0.02D
            );
            level.sendParticles(
                PathogenZoneManager.PATHOGEN_DUST,
                x + 0.5D,
                y + 0.3D,
                z + 0.5D,
                3,
                0.3D,
                0.1D,
                0.3D,
                0.01D
            );
            pulses++;
        }
        var center = Vec3.atCenterOf(origin);
        var spread = Math.max(2.0D, zone.radius() * 0.3D);
        level.sendParticles(
            PathogenZoneManager.PATHOGEN_DUST,
            center.x,
            center.y + 0.5D,
            center.z,
            40,
            spread,
            0.4D,
            spread,
            0.01D
        );
    }

    private static boolean isFlora(BlockState state) {
        return state.is(PathogenTags.Blocks.PATHOGEN_GROWTH) || state.is(PathogenTags.Blocks.SPORE_PLANTS);
    }
}
