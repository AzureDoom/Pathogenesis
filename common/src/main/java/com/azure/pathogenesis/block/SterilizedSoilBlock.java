package com.azure.pathogenesis.block;

import com.azure.pathogenesis.Pathogenesis;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class SterilizedSoilBlock extends Block {

    public SterilizedSoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            var duration = Pathogenesis.getConfig().containmentConfigs.sterilizedSoilDuration;
            level.scheduleTick(pos, this, duration + level.random.nextInt(Math.max(1, duration / 4)));
        }
    }

    @Override
    protected void tick(
        @NotNull BlockState state,
        ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
    }
}
