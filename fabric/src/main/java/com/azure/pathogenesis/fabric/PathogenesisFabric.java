package com.azure.pathogenesis.fabric;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.command.PathogenCommands;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenCreativeTabs;
import com.azure.pathogenesis.registry.PathogenEntities;
import com.azure.pathogenesis.registry.PathogenItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;

public final class PathogenesisFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Pathogenesis.init();
        PathogenItems.registerDispenserBehaviors();
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
    }
}
