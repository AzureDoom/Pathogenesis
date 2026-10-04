package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.Pathogenesis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

public final class ShorelineSpread {

    private ShorelineSpread() {}

    public static void randomTick(ServerLevel level, BlockPos pos, RandomSource random) {
        var config = Pathogenesis.getConfig().contaminationConfigs;
        if (!config.pathogenSpreadEnabled || !config.shorelineSpreadEnabled) {
            return;
        }
        var zone = PathogenZoneManager.findZone(level, pos);
        if (zone == null || config.shorelineRequiresEstablished && !zone.stage().isEstablished()) {
            return;
        }
        var now = level.getGameTime();
        var chance = config.shorelineSpreadChance
            * PathogenClimate.waterSpreadMultiplier(level, pos)
            * PathogenClimate.zoneActivity(zone, now)
            * (zone.isChilled() ? 1.0D : PathogenClimate.localSpreadMultiplier(level, pos));
        if (random.nextDouble() >= chance) {
            return;
        }

        var target = pickTarget(pos, random);
        if (!withinReach(zone, target, config.shorelineReach) || !level.isLoaded(target)) {
            return;
        }
        var state = level.getBlockState(target);
        if (state.is(Blocks.WATER)) {
            if (
                !state.getFluidState().isSource() || random.nextDouble() >= config.shorelineWaterCreep
                    || level.getBiome(target).is(BiomeTags.IS_OCEAN)
                    || level.getBiome(target).is(BiomeTags.IS_DEEP_OCEAN)
            ) {
                return;
            }
        } else if (!ContaminationPalette.canContaminate(state)) {
            return;
        }
        PathogenZoneManager.contaminate(level, zone, target, config.pathogenMaxRadius);
    }

    private static BlockPos pickTarget(BlockPos pos, RandomSource random) {
        var roll = random.nextInt(9);
        if (roll == 8) {
            return pos.below();
        }
        var side = pos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
        return roll < 4 ? side : side.above();
    }

    private static boolean withinReach(PathogenZone zone, BlockPos pos, int reach) {
        var origin = zone.origin();
        var dx = pos.getX() - origin.getX();
        var dz = pos.getZ() - origin.getZ();
        var limit = zone.radius() + Math.min(reach, PathogenZone.EDGE_MARGIN);
        return dx * dx + dz * dz <= limit * limit && Math.abs(
            pos.getY() - origin.getY()
        ) <= PathogenZone.VERTICAL_REACH;
    }
}
