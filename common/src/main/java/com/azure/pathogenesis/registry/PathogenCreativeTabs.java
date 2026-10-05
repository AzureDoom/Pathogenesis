package com.azure.pathogenesis.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PathogenCreativeTabs {

    private static final Map<ResourceKey<CreativeModeTab>, List<Supplier<ItemStack>>> TABS = new LinkedHashMap<>();

    private static final ResourceKey<CreativeModeTab> BUILDING_BLOCKS = vanillaTab("building_blocks");

    private static final ResourceKey<CreativeModeTab> INGREDIENTS = vanillaTab("ingredients");

    private static final ResourceKey<CreativeModeTab> SPAWN_EGGS = vanillaTab("spawn_eggs");

    private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES = vanillaTab("tools_and_utilities");

    static {
        tab(
            SPAWN_EGGS,
            item(PathogenItems.BLOODBURSTER_SPAWN_EGG),
            item(PathogenItems.NEOPHYTE_SPAWN_EGG),
            item(PathogenItems.NEOMORPH_SPAWN_EGG),
            item(PathogenItems.HAMMERPEDE_SPAWN_EGG),
            item(PathogenItems.PATHOGEN_POPPER_SPAWN_EGG)
        );
        tab(
            BUILDING_BLOCKS,
            item(PathogenItems.CONTAMINATED_DIRT),
            item(PathogenItems.CONTAMINATED_GRASS),
            item(PathogenItems.CONTAMINATED_MOSS),
            item(PathogenItems.CONTAMINATED_SNOW),
            item(PathogenItems.CONTAMINATED_SNOW_BLOCK),
            item(PathogenItems.CONTAMINATED_ICE),
            item(PathogenItems.CONTAMINATED_ROOTS),
            item(PathogenItems.PATHOGEN_GROWTH),
            item(PathogenItems.PATHOGEN_FUNGUS),
            item(PathogenItems.SPORE_PLANT),
            item(PathogenItems.STERILIZED_SOIL),
            item(PathogenItems.PATHOGEN_SOURCE)
        );
        tab(
            TOOLS_AND_UTILITIES,
            item(PathogenItems.PATHOGEN_SAMPLER)
        );
        tab(
            INGREDIENTS,
            item(PathogenItems.SEALED_PATHOGEN_AMPULE)
        );
    }

    private PathogenCreativeTabs() {}

    public static Set<ResourceKey<CreativeModeTab>> tabs() {
        return TABS.keySet();
    }

    public static void fill(ResourceKey<CreativeModeTab> tab, Consumer<ItemStack> output) {
        var entries = TABS.get(tab);
        if (entries == null) {
            return;
        }
        for (var entry : entries) {
            output.accept(entry.get());
        }
    }

    @SafeVarargs
    private static void tab(ResourceKey<CreativeModeTab> tab, Supplier<ItemStack>... entries) {
        TABS.computeIfAbsent(tab, key -> new java.util.ArrayList<>()).addAll(List.of(entries));
    }

    private static Supplier<ItemStack> item(Supplier<? extends ItemLike> item) {
        return () -> new ItemStack(item.get());
    }

    private static ResourceKey<CreativeModeTab> vanillaTab(String name) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.withDefaultNamespace(name));
    }
}
