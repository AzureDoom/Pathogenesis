package com.azure.pathogenesis.loot;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Set;

public final class PathogenLoot {

    public static final Set<ResourceKey<LootTable>> AMPULE_ARCHAEOLOGY_TABLES = Set.of(
        BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY,
        BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY,
        BuiltInLootTables.OCEAN_RUIN_WARM_ARCHAEOLOGY
    );

    public static final float AMPULE_ARCHAEOLOGY_CHANCE = 0.02F;

    private PathogenLoot() {}
}
