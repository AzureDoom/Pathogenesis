package com.azure.pathogenesis.registry;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.item.PathogenAmpuleItem;
import com.azure.pathogenesis.item.PathogenSamplerItem;
import com.azure.pathogenesis.item.PathogenSourceItem;
import com.azure.pathogenesis.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;

import java.util.function.Supplier;

public final class PathogenItems {

    public static final Supplier<PathogenAmpuleItem> SEALED_PATHOGEN_AMPULE = register(
        "sealed_pathogen_ampule",
        () -> new PathogenAmpuleItem(new Item.Properties().stacksTo(4).rarity(Rarity.EPIC))
    );

    public static final Supplier<PathogenSamplerItem> PATHOGEN_SAMPLER = register(
        "pathogen_sampler",
        () -> new PathogenSamplerItem(
            new Item.Properties().durability(Pathogenesis.getConfig().samplerConfigs.samplerDurability)
        )
    );

    public static final Supplier<PathogenSourceItem> PATHOGEN_SOURCE = register(
        "pathogen_source",
        () -> new PathogenSourceItem(
            PathogenBlocks.PATHOGEN_SOURCE.get(),
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
        )
    );

    public static final Supplier<BlockItem> CONTAMINATED_DIRT = block(
        "contaminated_dirt",
        PathogenBlocks.CONTAMINATED_DIRT
    );

    public static final Supplier<BlockItem> CONTAMINATED_GRASS = block(
        "contaminated_grass",
        PathogenBlocks.CONTAMINATED_GRASS
    );

    public static final Supplier<BlockItem> CONTAMINATED_MOSS = block(
        "contaminated_moss",
        PathogenBlocks.CONTAMINATED_MOSS
    );

    public static final Supplier<BlockItem> CONTAMINATED_SNOW = block(
        "contaminated_snow",
        PathogenBlocks.CONTAMINATED_SNOW
    );

    public static final Supplier<BlockItem> CONTAMINATED_SNOW_BLOCK = block(
        "contaminated_snow_block",
        PathogenBlocks.CONTAMINATED_SNOW_BLOCK
    );

    public static final Supplier<BlockItem> CONTAMINATED_ICE = block(
        "contaminated_ice",
        PathogenBlocks.CONTAMINATED_ICE
    );

    public static final Supplier<BlockItem> CONTAMINATED_ROOTS = block(
        "contaminated_roots",
        PathogenBlocks.CONTAMINATED_ROOTS
    );

    public static final Supplier<BlockItem> PATHOGEN_GROWTH = block("pathogen_growth", PathogenBlocks.PATHOGEN_GROWTH);

    public static final Supplier<BlockItem> PATHOGEN_FUNGUS = block("pathogen_fungus", PathogenBlocks.PATHOGEN_FUNGUS);

    public static final Supplier<BlockItem> SPORE_PLANT = block("spore_plant", PathogenBlocks.SPORE_PLANT);

    public static final Supplier<BlockItem> STERILIZED_SOIL = block("sterilized_soil", PathogenBlocks.STERILIZED_SOIL);

    public static final Supplier<Item> BLOODBURSTER_SPAWN_EGG = tab(
        Services.REGISTRY.registerSpawnEgg("bloodburster_spawn_egg", PathogenEntities.BLOODBURSTER, 0xFFFFFF, 0xFFFFFF)
    );

    public static final Supplier<Item> NEOPHYTE_SPAWN_EGG = tab(
        Services.REGISTRY.registerSpawnEgg("neophyte_spawn_egg", PathogenEntities.NEOPHYTE, 0xFFFFFF, 0xFFFFFF)
    );

    public static final Supplier<Item> NEOMORPH_SPAWN_EGG = tab(
        Services.REGISTRY.registerSpawnEgg("neomorph_spawn_egg", PathogenEntities.NEOMORPH, 0xFFFFFF, 0xFFFFFF)
    );

    public static final Supplier<Item> HAMMERPEDE_SPAWN_EGG = tab(
        Services.REGISTRY.registerSpawnEgg("hammerpede_spawn_egg", PathogenEntities.HAMMERPEDE, 0xFFFFFF, 0xFFFFFF)
    );

    public static final Supplier<Item> PATHOGEN_POPPER_SPAWN_EGG = tab(
        Services.REGISTRY.registerSpawnEgg(
            "pathogen_popper_spawn_egg",
            PathogenEntities.PATHOGEN_POPPER,
            0xFFFFFF,
            0xFFFFFF
        )
    );

    private PathogenItems() {}

    private static <I extends Item> Supplier<I> register(String name, Supplier<I> factory) {
        return tab(Services.REGISTRY.register(Registries.ITEM, name, factory));
    }

    private static Supplier<BlockItem> block(String name, Supplier<? extends Block> block) {
        return register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <I extends Item> Supplier<I> tab(Supplier<I> supplier) {
        return supplier;
    }

    public static void init() {}

    public static void registerDispenserBehaviors() {
        DispenserBlock.registerProjectileBehavior(SEALED_PATHOGEN_AMPULE.get());
    }

    public static void registerAzIdentity() {}
}
