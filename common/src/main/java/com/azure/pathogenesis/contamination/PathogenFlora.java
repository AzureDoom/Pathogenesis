package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;

final class PathogenFlora {

    private PathogenFlora() {}

    static boolean tryGrow(ServerLevel level, PathogenZone zone, BlockPos soilPos, RandomSource random) {
        var stage = zone.stage();
        if (!stage.allowsFlora()) {
            return false;
        }
        var rate = Pathogenesis.getConfig().floraConfigs.pathogenPlantGrowthRate
            * PathogenClimate.zoneFloraActivity(zone, level.getGameTime());
        var above = soilPos.above();
        var aboveState = level.getBlockState(above);

        if (aboveState.isAir()) {
            if (random.nextDouble() < 0.10D * rate) {
                return level.setBlock(
                    above,
                    PathogenBlocks.PATHOGEN_GROWTH.get().defaultBlockState(),
                    Block.UPDATE_ALL
                );
            }
            return false;
        }

        if (aboveState.is(PathogenBlocks.PATHOGEN_GROWTH.get())) {
            if (
                stage.allowsSpores()
                    && random.nextDouble() < 0.04D * rate
                    && countSporePlants(level, above) <= 0
            ) {
                return level.setBlock(above, PathogenBlocks.SPORE_PLANT.get().defaultBlockState(), Block.UPDATE_ALL);
            }
            if (stage.isEstablished() && random.nextDouble() < 0.02D * rate) {
                return level.setBlock(
                    above,
                    PathogenBlocks.PATHOGEN_FUNGUS.get().defaultBlockState(),
                    Block.UPDATE_ALL
                );
            }
            return false;
        }

        if (
            aboveState.is(PathogenTags.Blocks.CONTAMINATABLE_PLANTS) && ContaminationPalette.canContaminate(aboveState)
        ) {
            return ContaminationPalette.contaminate(level, above, aboveState);
        }
        return false;
    }

    private static int countSporePlants(ServerLevel level, BlockPos center) {
        var count = 0;
        for (
            var pos : BlockPos.betweenClosed(
                center.offset(-3, -1, -3),
                center.offset(3, 1, 3)
            )
        ) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(PathogenTags.Blocks.SPORE_PLANTS)) {
                count++;
            }
        }
        return count;
    }
}
