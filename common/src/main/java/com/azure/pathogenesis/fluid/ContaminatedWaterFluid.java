package com.azure.pathogenesis.fluid;

import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Water that the Pathogen has converted. Behaves like water for flow speed and physics (it is in
 * {@code #minecraft:water}), but never forms new sources, so contamination only advances when the spread controller
 * converts a real water source.
 * <p>
 * {@link Source} and {@link Flowing} are deliberately non-final: NeoForge subclasses them to supply a
 * {@code FluidType}. Always construct them through {@code Services.PLATFORM}.
 */
public abstract class ContaminatedWaterFluid extends FlowingFluid {

    @Override
    public @NotNull Fluid getFlowing() {
        return PathogenFluids.FLOWING_CONTAMINATED_WATER.get();
    }

    @Override
    public @NotNull Fluid getSource() {
        return PathogenFluids.CONTAMINATED_WATER.get();
    }

    @Override
    public @NotNull Item getBucket() {
        return Items.AIR;
    }

    @Override
    protected boolean canConvertToSource(@NotNull Level level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(@NotNull LevelAccessor level, @NotNull BlockPos pos, BlockState state) {
        var blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockEntity);
    }

    @Override
    protected int getSlopeFindDistance(@NotNull LevelReader level) {
        return 4;
    }

    @Override
    protected int getDropOff(@NotNull LevelReader level) {
        return 1;
    }

    @Override
    public int getTickDelay(@NotNull LevelReader level) {
        return 5;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean canBeReplacedWith(
        @NotNull FluidState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull Fluid fluid,
        @NotNull Direction direction
    ) {
        return direction == Direction.DOWN && !fluid.is(FluidTags.WATER);
    }

    @Override
    protected @NotNull BlockState createLegacyBlock(@NotNull FluidState state) {
        return PathogenBlocks.CONTAMINATED_WATER.get()
            .defaultBlockState()
            .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(@NotNull Fluid fluid) {
        return fluid == getSource() || fluid == getFlowing();
    }

    @Override
    public @NotNull Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL);
    }

    @Override
    public void animateTick(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull FluidState state,
        @NotNull RandomSource random
    ) {
        if (state.isSource() && random.nextInt(40) == 0 && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(
                PathogenZoneManager.PATHOGEN_DUST,
                pos.getX() + random.nextDouble(),
                pos.getY() + 0.95D,
                pos.getZ() + random.nextDouble(),
                0.0D,
                0.01D,
                0.0D
            );
        }
    }

    public static class Source extends ContaminatedWaterFluid {

        @Override
        public int getAmount(@NotNull FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(@NotNull FluidState state) {
            return true;
        }
    }

    public static class Flowing extends ContaminatedWaterFluid {

        @Override
        protected void createFluidStateDefinition(StateDefinition.@NotNull Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(@NotNull FluidState state) {
            return false;
        }
    }
}
