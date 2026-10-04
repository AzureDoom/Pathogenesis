package com.azure.pathogenesis.block;

import com.azure.pathogenesis.client.outbreak.ClientOutbreakState;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.exposure.ExposureType;
import com.azure.pathogenesis.exposure.PathogenExposureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ContaminatedSoilBlock extends Block {

    public ContaminatedSoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Entity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof LivingEntity living) {
            PathogenExposureHelper.contact(living, ExposureType.ENVIRONMENTAL);
            if (
                !entity.isSteppingCarefully()
                    && entity.getDeltaMovement().horizontalDistanceSqr() > 0.0025D
                    && (level.getGameTime() + entity.getId()) % 5 == 0
            ) {
                SporePlantBlock.disturbNearby(serverLevel, pos.above(), 1);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        RandomSource random
    ) {
        if (
            ClientOutbreakState.roll(level, pos, 1.0F / 24.0F, random.nextFloat())
                && level.getBlockState(pos.above()).isAir()
        ) {
            level.addParticle(
                PathogenZoneManager.PATHOGEN_DUST,
                pos.getX() + random.nextDouble(),
                pos.getY() + 1.05D,
                pos.getZ() + random.nextDouble(),
                0.0D,
                0.01D,
                0.0D
            );
        }
    }
}
