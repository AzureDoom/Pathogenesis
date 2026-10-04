package com.azure.pathogenesis.block;

import com.azure.pathogenesis.client.outbreak.ClientOutbreakState;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class PathogenFungusBlock extends PathogenGrowthBlock {

    public static final MapCodec<PathogenFungusBlock> CODEC = simpleCodec(PathogenFungusBlock::new);

    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 7.0D, 12.0D);

    public PathogenFungusBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull VoxelShape getShape(
        BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }

    @Override
    public void animateTick(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        RandomSource random
    ) {
        if (ClientOutbreakState.roll(level, pos, 1.0F / 12.0F, random.nextFloat())) {
            level.addParticle(
                PathogenZoneManager.PATHOGEN_DUST,
                pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.5D,
                0.0D,
                0.015D,
                0.0D
            );
        }
    }
}
