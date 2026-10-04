package com.azure.pathogenesis.block;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.contamination.PathogenClimate;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ContaminatedIceBlock extends IceBlock {

    public static final MapCodec<ContaminatedIceBlock> CODEC = simpleCodec(ContaminatedIceBlock::new);

    public ContaminatedIceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull MapCodec<? extends IceBlock> codec() {
        return CODEC;
    }

    public static BlockState thawsInto() {
        return PathogenBlocks.CONTAMINATED_WATER.get().defaultBlockState();
    }

    public static boolean tryFreeze(ServerLevel level, BlockPos pos, RandomSource random) {
        var climate = Pathogenesis.getConfig().climateConfigs;
        if (!climate.contaminatedWaterFreezes || !PathogenClimate.canFreezeContaminatedWater(level, pos)) {
            return false;
        }
        if (random.nextDouble() >= climate.contaminatedWaterFreezeChance) {
            return false;
        }
        return level.setBlock(pos, PathogenBlocks.CONTAMINATED_ICE.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public void playerDestroy(
        @NotNull Level level,
        @NotNull Player player,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @Nullable BlockEntity blockEntity,
        @NotNull ItemStack tool
    ) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (level.getBlockState(pos).is(Blocks.WATER)) {
            level.setBlockAndUpdate(pos, thawsInto());
        }
    }

    @Override
    protected void randomTick(
        @NotNull BlockState state,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        var lit = level.getBrightness(LightLayer.BLOCK, pos) > 11 - state.getLightBlock(level, pos);
        if (lit || PathogenClimate.isWarmEnoughToThaw(level, pos) && random.nextInt(3) == 0) {
            melt(state, level, pos);
        }
    }

    @Override
    protected void melt(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        if (level.dimensionType().ultraWarm()) {
            level.removeBlock(pos, false);
            return;
        }
        var water = thawsInto();
        level.setBlockAndUpdate(pos, water);
        level.neighborChanged(pos, water.getBlock(), pos);
        if (level instanceof ServerLevel serverLevel) {
            var c = Vec3.atCenterOf(pos);
            serverLevel.sendParticles(
                PathogenZoneManager.PATHOGEN_DUST,
                c.x,
                c.y + 0.5D,
                c.z,
                3,
                0.3D,
                0.1D,
                0.3D,
                0.0D
            );
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER, c.x, c.y + 0.4D, c.z, 2, 0.3D, 0.05D, 0.3D, 0.0D);
        }
    }
}
