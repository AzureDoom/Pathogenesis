package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ContaminationPalette {

    private ContaminationPalette() {}

    public static boolean canContaminate(BlockState state) {
        return state.is(PathogenTags.Blocks.CONTAMINATABLE) && !state.is(PathogenTags.Blocks.PATHOGEN_IMMUNE);
    }

    public static boolean contaminate(ServerLevel level, BlockPos pos, BlockState state) {
        var result = convert(level, pos, state);
        if (result == null) {
            return false;
        }
        return level.setBlock(pos, result, Block.UPDATE_ALL);
    }

    private static BlockState convert(ServerLevel level, BlockPos pos, BlockState state) {
        if (!canContaminate(state)) {
            return null;
        }
        var config = Pathogenesis.getConfig().contaminationConfigs;
        if (state.is(Blocks.WATER)) {
            return config.contaminateWater && state.getFluidState().isSource()
                ? PathogenBlocks.CONTAMINATED_WATER.get().defaultBlockState()
                : null;
        }
        if (state.is(Blocks.SNOW)) {
            return config.contaminateSnow
                ? PathogenBlocks.CONTAMINATED_SNOW.get()
                    .defaultBlockState()
                    .setValue(SnowLayerBlock.LAYERS, state.getValue(SnowLayerBlock.LAYERS))
                : null;
        }
        if (state.is(Blocks.SNOW_BLOCK)) {
            return config.contaminateSnow ? PathogenBlocks.CONTAMINATED_SNOW_BLOCK.get().defaultBlockState() : null;
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)) {
            return PathogenBlocks.CONTAMINATED_GRASS.get().defaultBlockState();
        }
        if (state.is(Blocks.MOSS_BLOCK)) {
            return PathogenBlocks.CONTAMINATED_MOSS.get().defaultBlockState();
        }
        if (state.is(Blocks.HANGING_ROOTS)) {
            return PathogenBlocks.CONTAMINATED_ROOTS.get().defaultBlockState();
        }
        if (state.is(PathogenTags.Blocks.CONTAMINATABLE_SOIL)) {
            return PathogenBlocks.CONTAMINATED_DIRT.get().defaultBlockState();
        }
        if (state.is(PathogenTags.Blocks.CONTAMINATABLE_PLANTS)) {
            var below = level.getBlockState(pos.below());
            return below.is(PathogenTags.Blocks.CONTAMINATED_SOIL)
                ? PathogenBlocks.PATHOGEN_GROWTH.get().defaultBlockState()
                : Blocks.AIR.defaultBlockState();
        }
        return null;
    }

    /**
     * What a contaminated block becomes when sterilized, or {@code null} to leave it alone. Snow and water go back to
     * their clean vanilla forms instead of being deleted, so fire or /pathogen sterilize never drains a lake or strips
     * a snowfield.
     */
    @Nullable
    public static BlockState sterilizedForm(BlockState state) {
        if (state.is(PathogenBlocks.CONTAMINATED_WATER.get())) {
            return state.getFluidState().isSource() ? Blocks.WATER.defaultBlockState() : null;
        }
        if (state.is(PathogenBlocks.CONTAMINATED_SNOW.get())) {
            return Blocks.SNOW.defaultBlockState()
                .setValue(SnowLayerBlock.LAYERS, state.getValue(SnowLayerBlock.LAYERS));
        }
        if (state.is(PathogenBlocks.CONTAMINATED_SNOW_BLOCK.get())) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }
        if (state.is(PathogenTags.Blocks.CONTAMINATED_SOIL)) {
            return PathogenBlocks.STERILIZED_SOIL.get().defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }
}
