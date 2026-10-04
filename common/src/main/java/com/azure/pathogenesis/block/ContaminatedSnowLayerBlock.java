package com.azure.pathogenesis.block;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.entity.SporeCloudEntity;
import com.azure.pathogenesis.exposure.ExposureType;
import com.azure.pathogenesis.exposure.PathogenExposureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ContaminatedSnowLayerBlock extends SnowLayerBlock {

    public ContaminatedSnowLayerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(
        @NotNull BlockState state,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        if (level.getBrightness(LightLayer.BLOCK, pos) > 11) {
            thaw(state, level, pos, random);
        }
    }

    private static void thaw(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
        var c = Vec3.atBottomCenterOf(pos);
        var layers = (int) state.getValue(LAYERS);
        level.sendParticles(
            PathogenZoneManager.PATHOGEN_DUST,
            c.x,
            c.y + 0.2D,
            c.z,
            6 + layers * 2,
            0.4D,
            0.15D,
            0.4D,
            0.01D
        );
        level.sendParticles(ParticleTypes.DRIPPING_WATER, c.x, c.y + 0.1D, c.z, 4, 0.3D, 0.05D, 0.3D, 0.0D);
        level.playSound(null, pos, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 0.6F, 0.7F);
        PathogenExposureHelper.exposeArea(
            level,
            c.add(0.0D, 0.5D, 0.0D),
            1.5D,
            2 + layers,
            ExposureType.ENVIRONMENTAL
        );

        var zone = PathogenZoneManager.findZone(level, pos);
        if (zone == null) {
            return;
        }
        PathogenZoneManager.contaminate(
            level,
            zone,
            pos.below(),
            Pathogenesis.getConfig().contaminationConfigs.pathogenMaxRadius
        );
        if (zone.stage().allowsSpores() && random.nextFloat() < 0.2F) {
            SporeCloudEntity.spawn(level, c.add(0.0D, 0.3D, 0.0D), zone.id());
        }
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
