package com.azure.pathogenesis.client;

import com.azure.pathogenesis.item.sampler.SampleResult;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenDataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class PathogenColors {

    public static final int CONTAMINATED_SOIL = 0xFF4A4048;

    public static final int CONTAMINATED_GRASS = 0xFF3B4636;

    public static final int CONTAMINATED_MOSS = 0xFF3E3E42;

    public static final int STERILIZED_SOIL = 0xFF8C8682;

    public static final int CONTAMINATED_ROOTS = 0xFF3A3034;

    public static final int PATHOGEN_GROWTH = 0xFF2A2F28;

    public static final int PATHOGEN_FUNGUS = 0xFF5A5660;

    public static final int CONTAMINATED_SNOW = 0xFF9C95A2;

    public static final int CONTAMINATED_WATER = 0xFF2E2A34;

    public static final int CONTAMINATED_ICE = 0xFFB9B3C2;

    private PathogenColors() {}

    public static int tint(Block block, int tintIndex) {
        if (block == PathogenBlocks.CONTAMINATED_GRASS.get()) {
            return tintIndex == 1 ? CONTAMINATED_GRASS : CONTAMINATED_SOIL;
        }
        if (
            block == PathogenBlocks.CONTAMINATED_SNOW.get() || block == PathogenBlocks.CONTAMINATED_SNOW_BLOCK.get()
        ) {
            return CONTAMINATED_SNOW;
        }
        if (block == PathogenBlocks.CONTAMINATED_ICE.get()) {
            return CONTAMINATED_ICE;
        }
        if (block == PathogenBlocks.CONTAMINATED_MOSS.get()) {
            return CONTAMINATED_MOSS;
        }
        if (block == PathogenBlocks.STERILIZED_SOIL.get()) {
            return STERILIZED_SOIL;
        }
        if (block == PathogenBlocks.CONTAMINATED_ROOTS.get()) {
            return CONTAMINATED_ROOTS;
        }
        if (block == PathogenBlocks.PATHOGEN_GROWTH.get()) {
            return PATHOGEN_GROWTH;
        }
        if (block == PathogenBlocks.PATHOGEN_FUNGUS.get()) {
            return PATHOGEN_FUNGUS;
        }
        return CONTAMINATED_SOIL;
    }

    /**
     * Pathogen Sampler: layer 0 (body) is untinted, layer 1 (vial contents) shows the last reading.
     */
    public static int sampler(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) {
            return -1;
        }
        var last = stack.get(PathogenDataComponents.SAMPLE_RESULT.get());
        return (last == null ? SampleResult.CLEAN : last).vialColor;
    }

    public static Block[] tintedBlocks() {
        return new Block[] {
            PathogenBlocks.CONTAMINATED_DIRT.get(),
            PathogenBlocks.CONTAMINATED_GRASS.get(),
            PathogenBlocks.CONTAMINATED_MOSS.get(),
            PathogenBlocks.CONTAMINATED_SNOW.get(),
            PathogenBlocks.CONTAMINATED_SNOW_BLOCK.get(),
            PathogenBlocks.CONTAMINATED_ICE.get(),
            PathogenBlocks.CONTAMINATED_ROOTS.get(),
            PathogenBlocks.PATHOGEN_GROWTH.get(),
            PathogenBlocks.PATHOGEN_FUNGUS.get(),
            PathogenBlocks.STERILIZED_SOIL.get()
        };
    }
}
