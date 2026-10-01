package com.azure.pathogenesis.neoforge.client;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.client.PathogenColors;
import com.azure.pathogenesis.client.dispatch.PathogenClient;
import com.azure.pathogenesis.client.sound.ContaminationAmbience;
import com.azure.pathogenesis.registry.PathogenItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = Pathogenesis.MOD_ID, value = Dist.CLIENT)
public final class PathogenesisNeoForgeClient {

    private PathogenesisNeoForgeClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        PathogenClient.registerRenderers(event::registerEntityRenderer);
    }

    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
            (state, level, pos, tintIndex) -> PathogenColors.tint(state.getBlock(), tintIndex),
            PathogenColors.tintedBlocks()
        );
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
            (stack, tintIndex) -> PathogenColors.tint(((BlockItem) stack.getItem()).getBlock(), tintIndex),
            PathogenColors.tintedBlocks()
        );
        event.register(
            (stack, tintIndex) -> PathogenColors.ampuleTint(tintIndex),
            PathogenItems.SEALED_PATHOGEN_AMPULE.get()
        );
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        ContaminationAmbience.tick(Minecraft.getInstance());
    }
}
