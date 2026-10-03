package com.azure.pathogenesis.fabric;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.command.PathogenCommands;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenEntities;
import com.azure.pathogenesis.registry.PathogenItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.world.item.CreativeModeTabs;

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
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
            entries.accept(PathogenItems.BLOODBURSTER_SPAWN_EGG.get());
            entries.accept(PathogenItems.NEOPHYTE_SPAWN_EGG.get());
            entries.accept(PathogenItems.NEOMORPH_SPAWN_EGG.get());
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.accept(PathogenItems.CONTAMINATED_DIRT.get());
            entries.accept(PathogenItems.CONTAMINATED_GRASS.get());
            entries.accept(PathogenItems.CONTAMINATED_MOSS.get());
            entries.accept(PathogenItems.CONTAMINATED_SNOW.get());
            entries.accept(PathogenItems.CONTAMINATED_SNOW_BLOCK.get());
            entries.accept(PathogenItems.CONTAMINATED_ROOTS.get());
            entries.accept(PathogenItems.PATHOGEN_GROWTH.get());
            entries.accept(PathogenItems.PATHOGEN_FUNGUS.get());
            entries.accept(PathogenItems.SPORE_PLANT.get());
            entries.accept(PathogenItems.STERILIZED_SOIL.get());
            entries.accept(PathogenItems.PATHOGEN_SOURCE.get());
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
            .register(entries -> entries.accept(PathogenItems.SEALED_PATHOGEN_AMPULE.get()));
    }
}
