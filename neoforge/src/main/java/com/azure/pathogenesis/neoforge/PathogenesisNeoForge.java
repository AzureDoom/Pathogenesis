package com.azure.pathogenesis.neoforge;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.command.PathogenCommands;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.neoforge.platform.NeoForgeRegistryHelper;
import com.azure.pathogenesis.registry.PathogenEntities;
import com.azure.pathogenesis.registry.PathogenItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@Mod(Pathogenesis.MOD_ID)
public final class PathogenesisNeoForge {

    public PathogenesisNeoForge(IEventBus modBus) {
        Pathogenesis.init();
        NeoForgeRegistryHelper.attach(modBus);
        modBus.addListener(
            EntityAttributeCreationEvent.class,
            event -> PathogenEntities.registerAttributes(event::put)
        );

        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                PathogenZoneManager.tick(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> PathogenZoneManager.clearTransientState());
        NeoForge.EVENT_BUS.addListener(
            RegisterCommandsEvent.class,
            event -> PathogenCommands.register(event.getDispatcher())
        );
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(PathogenItems.BLOODBURSTER_SPAWN_EGG.get());
                event.accept(PathogenItems.NEOPHYTE_SPAWN_EGG.get());
                event.accept(PathogenItems.NEOMORPH_SPAWN_EGG.get());
            }
            if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
                event.accept(PathogenItems.CONTAMINATED_DIRT.get());
                event.accept(PathogenItems.CONTAMINATED_GRASS.get());
                event.accept(PathogenItems.CONTAMINATED_MOSS.get());
                event.accept(PathogenItems.CONTAMINATED_ROOTS.get());
                event.accept(PathogenItems.PATHOGEN_GROWTH.get());
                event.accept(PathogenItems.PATHOGEN_FUNGUS.get());
                event.accept(PathogenItems.SPORE_PLANT.get());
                event.accept(PathogenItems.STERILIZED_SOIL.get());
            }
            if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                event.accept(PathogenItems.SEALED_PATHOGEN_AMPULE.get());
            }
        });
    }
}
