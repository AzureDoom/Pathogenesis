package com.azure.pathogenesis.fabric;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.command.PathogenCommands;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.loot.PathogenLoot;
import com.azure.pathogenesis.network.OutbreakStatePayload;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenCreativeTabs;
import com.azure.pathogenesis.registry.PathogenEntities;
import com.azure.pathogenesis.registry.PathogenItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

public final class PathogenesisFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Pathogenesis.init();
        PayloadTypeRegistry.playS2C().register(OutbreakStatePayload.TYPE, OutbreakStatePayload.STREAM_CODEC);
        PathogenItems.registerDispenserBehaviors();
        PathogenItems.registerAzIdentity();
        PathogenEntities.registerAttributes(FabricDefaultAttributeRegistry::register);
        PathogenBlocks.flammableFlora()
            .forEach(block -> FlammableBlockRegistry.getDefaultInstance().add(block.get(), 60, 100));
        ServerTickEvents.END_WORLD_TICK.register(PathogenZoneManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PathogenZoneManager.clearTransientState());
        CommandRegistrationCallback.EVENT.register(
            (dispatcher, context, selection) -> PathogenCommands.register(dispatcher)
        );
        for (var tab : PathogenCreativeTabs.tabs()) {
            ItemGroupEvents.modifyEntriesEvent(tab)
                .register(entries -> PathogenCreativeTabs.fill(tab, entries::accept));
        }
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (source.isBuiltin() && PathogenLoot.AMPULE_ARCHAEOLOGY_TABLES.contains(key)) {
                tableBuilder.modifyPools(
                    pool -> pool.add(
                        LootItem.lootTableItem(PathogenItems.SEALED_PATHOGEN_AMPULE.get())
                            .when(LootItemRandomChanceCondition.randomChance(0.2F))
                    )
                );
            }
        });
    }
}
