package com.azure.pathogenesis.registry;

import com.azure.pathogenesis.block.ContaminatedRootsBlock;
import com.azure.pathogenesis.block.ContaminatedSoilBlock;
import com.azure.pathogenesis.block.PathogenFungusBlock;
import com.azure.pathogenesis.block.PathogenGrowthBlock;
import com.azure.pathogenesis.block.PathogenSourceBlock;
import com.azure.pathogenesis.block.SporePlantBlock;
import com.azure.pathogenesis.block.SterilizedSoilBlock;
import com.azure.pathogenesis.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.List;
import java.util.function.Supplier;

public final class PathogenBlocks {

    public static final Supplier<PathogenSourceBlock> PATHOGEN_SOURCE = register(
        "pathogen_source",
        () -> new PathogenSourceBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F, 2.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .lightLevel(state -> state.getValue(PathogenSourceBlock.CONTAINMENT).isLeaking() ? 3 : 0)
        )
    );

    public static final Supplier<ContaminatedSoilBlock> CONTAMINATED_DIRT = register(
        "contaminated_dirt",
        () -> new ContaminatedSoilBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(MapColor.COLOR_BLACK)
        )
    );

    public static final Supplier<ContaminatedSoilBlock> CONTAMINATED_GRASS = register(
        "contaminated_grass",
        () -> new ContaminatedSoilBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).mapColor(MapColor.TERRACOTTA_BLACK)
        )
    );

    public static final Supplier<ContaminatedSoilBlock> CONTAMINATED_MOSS = register(
        "contaminated_moss",
        () -> new ContaminatedSoilBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK).mapColor(MapColor.COLOR_GRAY)
        )
    );

    public static final Supplier<ContaminatedRootsBlock> CONTAMINATED_ROOTS = register(
        "contaminated_roots",
        () -> new ContaminatedRootsBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.HANGING_ROOTS).mapColor(MapColor.COLOR_BLACK)
        )
    );

    public static final Supplier<PathogenGrowthBlock> PATHOGEN_GROWTH = register(
        "pathogen_growth",
        () -> new PathogenGrowthBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .replaceable()
                .noCollission()
                .instabreak()
                .sound(SoundType.WET_GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY)
        )
    );

    public static final Supplier<PathogenFungusBlock> PATHOGEN_FUNGUS = register(
        "pathogen_fungus",
        () -> new PathogenFungusBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .noCollission()
                .strength(0.1F)
                .sound(SoundType.FUNGUS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY)
        )
    );

    public static final Supplier<SporePlantBlock> SPORE_PLANT = register(
        "spore_plant",
        () -> new SporePlantBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .noCollission()
                .strength(0.2F)
                .randomTicks()
                .sound(SoundType.FUNGUS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY)
                .lightLevel(state -> SporePlantBlock.isPrimed(state) ? 2 : 0)
        )
    );

    public static final Supplier<SterilizedSoilBlock> STERILIZED_SOIL = register(
        "sterilized_soil",
        () -> new SterilizedSoilBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.COARSE_DIRT).mapColor(MapColor.COLOR_GRAY)
        )
    );

    private PathogenBlocks() {}

    private static <B extends Block> Supplier<B> register(String name, Supplier<B> factory) {
        return Services.REGISTRY.register(Registries.BLOCK, name, factory);
    }

    public static List<Supplier<? extends Block>> cutoutBlocks() {
        return List.of(CONTAMINATED_ROOTS, PATHOGEN_GROWTH, PATHOGEN_FUNGUS, SPORE_PLANT, PATHOGEN_SOURCE);
    }

    public static List<Supplier<? extends Block>> flammableFlora() {
        return List.of(PATHOGEN_GROWTH, PATHOGEN_FUNGUS, SPORE_PLANT, CONTAMINATED_ROOTS);
    }

    public static void init() {}
}
