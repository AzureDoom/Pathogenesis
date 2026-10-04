package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class PathogenSpreadController {

    private static final int[][] NEIGHBORS = {
        { 1, 0, 0 },
        { -1, 0, 0 },
        { 0, 1, 0 },
        { 0, -1, 0 },
        { 0, 0, 1 },
        { 0, 0, -1 },
        { 1, 1, 0 },
        { -1, 1, 0 },
        { 0, 1, 1 },
        { 0, 1, -1 },
        { 1, -1, 0 },
        { -1, -1, 0 },
        { 0, -1, 1 },
        { 0, -1, -1 },
        { 1, 0, 1 },
        { 1, 0, -1 },
        { -1, 0, 1 },
        { -1, 0, -1 }
    };

    private PathogenSpreadController() {}

    public static boolean process(ServerLevel level, PathogenZone zone, int checks) {
        var config = Pathogenesis.getConfig();
        var random = level.random;
        var chance = 0.25D * config.contaminationConfigs.pathogenSpreadRate;
        var dieBack = zone.phase().diesBack() ? config.contaminationConfigs.collapseDieBackChance : 0.0D;
        var changed = false;

        for (var i = 0; i < checks; i++) {
            var candidate = pickCandidate(level, zone, random);
            if (candidate == null) {
                continue;
            }
            var state = level.getBlockState(candidate);

            var above = candidate.above();
            var aboveState = level.getBlockState(above);
            if (aboveState.is(Blocks.SNOW)) {
                candidate = above;
                state = aboveState;
            }

            if (dieBack > 0.0D && isActiveMaterial(state)) {
                if (random.nextDouble() < dieBack && recede(level, zone, candidate, state)) {
                    changed = true;
                }
                continue;
            }

            if (state.is(PathogenTags.Blocks.CONTAMINATED_SOIL)) {
                if (PathogenFlora.tryGrow(level, zone, candidate, random)) {
                    zone.addContamination(1, config.contaminationConfigs.pathogenMaxRadius);
                    changed = true;
                }
                continue;
            }

            if (!ContaminationPalette.canContaminate(state)) {
                continue;
            }
            var candidateChance = state.is(Blocks.WATER)
                ? chance * PathogenClimate.waterSpreadMultiplier(level, candidate)
                : chance;
            if (!zone.isChilled()) {
                candidateChance *= PathogenClimate.localSpreadMultiplier(level, candidate);
            }
            if (random.nextDouble() >= candidateChance) {
                continue;
            }
            if (!isConnected(level, zone, candidate) || !ContaminationPalette.contaminate(level, candidate, state)) {
                continue;
            }
            zone.addContamination(1, config.contaminationConfigs.pathogenMaxRadius);
            changed = true;
        }
        return changed;
    }

    private static boolean recede(ServerLevel level, PathogenZone zone, BlockPos pos, BlockState state) {
        if (!isFrontier(level, pos)) {
            return false;
        }
        var result = ContaminationPalette.recededForm(state);
        if (result == null || !level.setBlock(pos, result, Block.UPDATE_ALL)) {
            return false;
        }
        zone.addContamination(-1, Pathogenesis.getConfig().contaminationConfigs.pathogenMaxRadius);
        return true;
    }

    private static boolean isFrontier(ServerLevel level, BlockPos pos) {
        var cursor = new BlockPos.MutableBlockPos();
        for (var offset : NEIGHBORS) {
            cursor.setWithOffset(pos, offset[0], offset[1], offset[2]);
            if (level.isLoaded(cursor) && ContaminationPalette.canContaminate(level.getBlockState(cursor))) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos pickCandidate(ServerLevel level, PathogenZone zone, RandomSource random) {
        var origin = zone.origin();
        var angle = random.nextDouble() * Math.PI * 2.0D;
        var distance = Math.sqrt(random.nextDouble()) * zone.radius();
        var x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
        var z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return null;
        }
        var y = random.nextBoolean()
            ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1
            : origin.getY() + random.nextInt(17) - 8;
        if (Math.abs(y - origin.getY()) > PathogenZone.VERTICAL_REACH || level.isOutsideBuildHeight(y)) {
            return null;
        }
        return new BlockPos(x, y, z);
    }

    private static boolean isConnected(ServerLevel level, PathogenZone zone, BlockPos pos) {
        if (zone.isSourceActive() && pos.distSqr(zone.origin()) <= 9.0D) {
            return true;
        }
        var cursor = new BlockPos.MutableBlockPos();
        for (var offset : NEIGHBORS) {
            cursor.setWithOffset(pos, offset[0], offset[1], offset[2]);
            if (!level.isLoaded(cursor)) {
                continue;
            }
            var neighbor = level.getBlockState(cursor);
            if (isActiveMaterial(neighbor) || neighbor.is(PathogenBlocks.PATHOGEN_SOURCE.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isActiveMaterial(BlockState state) {
        return state.is(PathogenTags.Blocks.CONTAMINATED) && !state.is(PathogenTags.Blocks.DORMANT_CONTAMINATION);
    }
}
