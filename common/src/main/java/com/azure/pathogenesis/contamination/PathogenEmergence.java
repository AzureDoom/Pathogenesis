package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.entity.HammerpedeEntity;
import com.azure.pathogenesis.entity.PopperEntity;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenEntities;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PathogenEmergence {

    private static final int HAMMERPEDE_SAMPLES = 24;

    private static final double PLAYER_RANGE = 48.0D;

    private static final double POPPER_CAP_RADIUS = 16.0D;

    private static final Map<UUID, Long> NEXT_HAMMERPEDE = new HashMap<>();

    private static final Map<UUID, Disturbance> DISTURBANCE = new HashMap<>();

    private PathogenEmergence() {}

    public static void tickZone(ServerLevel level, PathogenZone zone, long now) {
        var config = Pathogenesis.getConfig().entityConfigs.hammerpedeConfigs;
        var next = NEXT_HAMMERPEDE.get(zone.id());
        if (next == null) {
            NEXT_HAMMERPEDE.put(zone.id(), now + config.hammerpedeEmergenceInterval);
            return;
        }
        if (now < next) {
            return;
        }
        NEXT_HAMMERPEDE.put(zone.id(), now + config.hammerpedeEmergenceInterval);
        if (
            !zone.stage().isEstablished() || zone.phase().diesBack() || config.hammerpedeMaxPerZone <= 0
                || level.random.nextDouble() >= config.hammerpedeEmergenceChance
        ) {
            return;
        }
        var origin = zone.origin();
        if (!level.hasNearbyAlivePlayer(origin.getX(), origin.getY(), origin.getZ(), zone.radius() + PLAYER_RANGE)) {
            return;
        }
        if (countHammerpedes(level, zone) >= config.hammerpedeMaxPerZone) {
            return;
        }
        var growth = findMatureGrowth(level, zone);
        if (growth != null) {
            spawnHammerpede(level, zone, growth);
        }
    }

    private static int countHammerpedes(ServerLevel level, PathogenZone zone) {
        var origin = Vec3.atCenterOf(zone.origin());
        var r = zone.radius() + PathogenZone.EDGE_MARGIN;
        var box = new AABB(origin, origin).inflate(r, PathogenZone.VERTICAL_REACH, r);
        return level.getEntitiesOfClass(
            HammerpedeEntity.class,
            box,
            hammerpede -> zone.id().equals(hammerpede.originZone()) || zone.isWithinRadius(hammerpede.blockPosition())
        ).size();
    }

    @Nullable
    private static BlockPos findMatureGrowth(ServerLevel level, PathogenZone zone) {
        var origin = zone.origin();
        var random = level.random;
        for (var i = 0; i < HAMMERPEDE_SAMPLES; i++) {
            var angle = random.nextDouble() * Math.PI * 2.0D;
            var distance = Math.sqrt(random.nextDouble()) * zone.radius();
            var x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
            var z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            var pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (
                Math.abs(pos.getY() - origin.getY()) <= PathogenZone.VERTICAL_REACH && level.getBlockState(pos)
                    .is(PathogenBlocks.PATHOGEN_FUNGUS.get())
            ) {
                return pos;
            }
        }
        return null;
    }

    private static void spawnHammerpede(ServerLevel level, PathogenZone zone, BlockPos pos) {
        var hammerpede = PathogenEntities.HAMMERPEDE.get().create(level);
        if (hammerpede == null) {
            return;
        }
        hammerpede.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        hammerpede.setOriginZone(zone.id());
        hammerpede.markEmerged();
        level.addFreshEntity(hammerpede);
        level.sendParticles(
            PathogenZoneManager.PATHOGEN_DUST,
            pos.getX() + 0.5D,
            pos.getY() + 0.2D,
            pos.getZ() + 0.5D,
            16,
            0.3D,
            0.1D,
            0.3D,
            0.02D
        );
        if (Pathogenesis.getConfig().debugLogging)
            Pathogenesis.LOGGER.debug("Hammerpede emerged from mature growth at {} in zone {}", pos, zone.id());
    }

    public static void onFloraBurned(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isFlora(state)) {
            return;
        }
        var zone = PathogenZoneManager.findZone(level, pos);
        if (zone == null || !zone.stage().allowsSpores()) {
            return;
        }
        if (level.random.nextDouble() < Pathogenesis.getConfig().entityConfigs.popperConfigs.popperBurnChance) {
            releasePopper(level, zone, pos, null);
        }
    }

    public static void onFloraBroken(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        if (!isFlora(state)) {
            return;
        }
        var zone = PathogenZoneManager.findZone(level, pos);
        if (zone == null || !zone.stage().isEstablished()) {
            return;
        }
        var config = Pathogenesis.getConfig().entityConfigs.popperConfigs;
        var now = level.getGameTime();
        var disturbance = DISTURBANCE.computeIfAbsent(zone.id(), id -> new Disturbance());
        if (now - disturbance.lastTick > config.popperDisturbanceWindow) {
            disturbance.count = 0;
        }
        disturbance.lastTick = now;
        if (++disturbance.count >= config.popperBreakThreshold && releasePopper(level, zone, pos, player)) {
            disturbance.count = 0;
        }
    }

    private static boolean isFlora(BlockState state) {
        return state.is(PathogenTags.Blocks.PATHOGEN_GROWTH) || state.is(PathogenTags.Blocks.SPORE_PLANTS);
    }

    private static boolean releasePopper(ServerLevel level, PathogenZone zone, BlockPos near, @Nullable Player cause) {
        var config = Pathogenesis.getConfig().entityConfigs.popperConfigs;
        var box = new AABB(near).inflate(POPPER_CAP_RADIUS);
        if (level.getEntitiesOfClass(PopperEntity.class, box).size() >= config.popperMaxNearby) {
            return false;
        }
        var spot = findReleaseSpot(level, near, cause);
        if (spot == null) {
            return false;
        }
        var popper = PathogenEntities.PATHOGEN_POPPER.get().create(level);
        if (popper == null) {
            return false;
        }
        popper.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        popper.setOriginZone(zone.id());
        popper.markEmerged();
        level.addFreshEntity(popper);
        level.sendParticles(
            PathogenZoneManager.PATHOGEN_DUST,
            spot.getX() + 0.5D,
            spot.getY() + 0.3D,
            spot.getZ() + 0.5D,
            20,
            0.4D,
            0.2D,
            0.4D,
            0.02D
        );
        return true;
    }

    @Nullable
    private static BlockPos findReleaseSpot(ServerLevel level, BlockPos near, @Nullable Player cause) {
        var random = level.random;
        BlockPos fallback = null;
        for (var i = 0; i < 12; i++) {
            var candidate = near.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
            if (!isOpenGround(level, candidate) || isNearFire(level, candidate)) {
                continue;
            }
            if (cause == null || candidate.distSqr(cause.blockPosition()) >= 9.0D) {
                return candidate;
            }
            if (fallback == null) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    private static boolean isOpenGround(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return (state.isAir() || state.canBeReplaced()) && level.getBlockState(pos.above()).isAir()
            && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
            && state.getFluidState().isEmpty();
    }

    private static boolean isNearFire(ServerLevel level, BlockPos pos) {
        for (var target : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(target).getBlock() instanceof BaseFireBlock) {
                return true;
            }
        }
        return false;
    }

    public static void forget(UUID zoneId) {
        NEXT_HAMMERPEDE.remove(zoneId);
        DISTURBANCE.remove(zoneId);
    }

    public static void clear() {
        NEXT_HAMMERPEDE.clear();
        DISTURBANCE.clear();
    }

    private static final class Disturbance {

        int count;

        long lastTick;
    }
}
