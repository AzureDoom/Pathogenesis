package com.azure.pathogenesis.block;

import com.azure.pathogenesis.exposure.ExposureType;
import com.azure.pathogenesis.exposure.PathogenExposureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ContaminatedWaterBlock extends LiquidBlock {

    public ContaminatedWaterBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Entity entity
    ) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            PathogenExposureHelper.contact(living, ExposureType.ENVIRONMENTAL);
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public @NotNull ItemStack pickupBlock(
        @Nullable Player player,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockState state
    ) {
        return ItemStack.EMPTY;
    }
}
