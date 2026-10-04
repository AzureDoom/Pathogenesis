package com.azure.pathogenesis.neoforge;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.client.outbreak.ClientOutbreakState;
import com.azure.pathogenesis.command.PathogenCommands;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.neoforge.platform.NeoForgeRegistryHelper;
import com.azure.pathogenesis.network.OutbreakStatePayload;
import com.azure.pathogenesis.registry.PathogenCreativeTabs;
import com.azure.pathogenesis.registry.PathogenEntities;
import com.azure.pathogenesis.registry.PathogenItems;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Pathogenesis.MOD_ID)
public final class PathogenesisNeoForge {

    public PathogenesisNeoForge(IEventBus modBus) {
        Pathogenesis.init();
        PathogenFluidTypes.FLUID_TYPES.register(modBus);
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
        modBus.addListener(
            RegisterPayloadHandlersEvent.class,
            event -> event.registrar("1")
                .playToClient(
                    OutbreakStatePayload.TYPE,
                    OutbreakStatePayload.STREAM_CODEC,
                    (payload, context) -> ClientOutbreakState.accept(payload)
                )
        );
        modBus.addListener(
            FMLCommonSetupEvent.class,
            event -> event.enqueueWork(PathogenItems::registerDispenserBehaviors)
        );
        modBus.addListener(
            BuildCreativeModeTabContentsEvent.class,
            event -> PathogenCreativeTabs.fill(event.getTabKey(), event::accept)
        );
    }
}
