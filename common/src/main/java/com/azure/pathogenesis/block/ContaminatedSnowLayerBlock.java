package com.azure.pathogenesis.block;

import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.exposure.ExposureType;
import com.azure.pathogenesis.exposure.PathogenExposureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ContaminatedSnowLayerBlock extends SnowLayerBlock {

    public ContaminatedSnowLayerBlock(Properties properties) {
        super(properties);
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
    public void animateTick(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        RandomSource random
    ) {
        if (random.nextInt(32) == 0 && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(
                PathogenZoneManager.PATHOGEN_DUST,
                pos.getX() + random.nextDouble(),
                pos.getY() + state.getValue(LAYERS) / 8.0D + 0.05D,
                pos.getZ() + random.nextDouble(),
                0.0D,
                0.01D,
                0.0D
            );
        }
    }
}
