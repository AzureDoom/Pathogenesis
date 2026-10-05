package com.azure.pathogenesis.client;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.item.sampler.SampleResult;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenDataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class PathogenColors {

    private PathogenColors() {}

    private static final class Colors {

        private static final int CONTAMINATED_SOIL;

        private static final int CONTAMINATED_GRASS;

        private static final int CONTAMINATED_MOSS;

        private static final int STERILIZED_SOIL;

        private static final int CONTAMINATED_ROOTS;

        private static final int PATHOGEN_GROWTH;

        private static final int PATHOGEN_FUNGUS;

        private static final int CONTAMINATED_SNOW;

        private static final int CONTAMINATED_WATER;

        private static final int CONTAMINATED_ICE;

        static {
            var c = Pathogenesis.config.colorConfigs;
            CONTAMINATED_SOIL = parse(c.contaminatedSoil, 0xFF4A4048);
            CONTAMINATED_GRASS = parse(c.contaminatedGrass, 0xFF3B4636);
            CONTAMINATED_MOSS = parse(c.contaminatedMoss, 0xFF3E3E42);
            STERILIZED_SOIL = parse(c.sterilizedSoil, 0xFF8C8682);
            CONTAMINATED_ROOTS = parse(c.contaminatedRoots, 0xFF3A3034);
            PATHOGEN_GROWTH = parse(c.pathogenGrowth, 0xFF2A2F28);
            PATHOGEN_FUNGUS = parse(c.pathogenFungus, 0xFF5A5660);
            CONTAMINATED_SNOW = parse(c.contaminatedSnow, 0xFF9C95A2);
            CONTAMINATED_WATER = parse(c.contaminatedWater, 0xFF2E2A34);
            CONTAMINATED_ICE = parse(c.contaminatedIce, 0xFFB9B3C2);
        }
    }

    private static int parse(String value, int fallback) {
        if (value == null)
            return fallback;
        var hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.isEmpty() || hex.length() > 8)
            return fallback;
        try {
            var parsed = (int) Long.parseLong(hex, 16);
            return hex.length() <= 6 ? 0xFF000000 | parsed : parsed;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static int contaminatedWater() {
        return Colors.CONTAMINATED_WATER;
    }

    public static int tint(Block block, int tintIndex) {
        if (block == PathogenBlocks.CONTAMINATED_GRASS.get()) {
            return tintIndex == 1 ? Colors.CONTAMINATED_GRASS : Colors.CONTAMINATED_SOIL;
        }
        if (
            block == PathogenBlocks.CONTAMINATED_SNOW.get() || block == PathogenBlocks.CONTAMINATED_SNOW_BLOCK.get()
        ) {
            return Colors.CONTAMINATED_SNOW;
        }
        if (block == PathogenBlocks.CONTAMINATED_ICE.get()) {
            return Colors.CONTAMINATED_ICE;
        }
        if (block == PathogenBlocks.CONTAMINATED_MOSS.get()) {
            return Colors.CONTAMINATED_MOSS;
        }
        if (block == PathogenBlocks.STERILIZED_SOIL.get()) {
            return Colors.STERILIZED_SOIL;
        }
        if (block == PathogenBlocks.CONTAMINATED_ROOTS.get()) {
            return Colors.CONTAMINATED_ROOTS;
        }
        if (block == PathogenBlocks.PATHOGEN_GROWTH.get()) {
            return Colors.PATHOGEN_GROWTH;
        }
        if (block == PathogenBlocks.PATHOGEN_FUNGUS.get()) {
            return Colors.PATHOGEN_FUNGUS;
        }
        return Colors.CONTAMINATED_SOIL;
    }

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
