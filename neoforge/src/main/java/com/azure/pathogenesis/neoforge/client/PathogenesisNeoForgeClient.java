package com.azure.pathogenesis.neoforge.client;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.client.PathogenColors;
import com.azure.pathogenesis.client.dispatch.PathogenClient;
import com.azure.pathogenesis.client.sound.ContaminationAmbience;
import com.azure.pathogenesis.neoforge.PathogenFluidTypes;
import com.azure.pathogenesis.registry.PathogenFluids;
import com.azure.pathogenesis.registry.PathogenItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = Pathogenesis.MOD_ID, value = Dist.CLIENT)
public final class PathogenesisNeoForgeClient {

    private PathogenesisNeoForgeClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        PathogenClient.registerRenderers(event::registerEntityRenderer);
        PathogenClient.registerBlockEntityRenderers(event::registerBlockEntityRenderer);
        PathogenClient.registerItemRenderers();
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
        event.register(PathogenColors::sampler, PathogenItems.PATHOGEN_SAMPLER.get());
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        ContaminationAmbience.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void clientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {

            @Override
            public @NotNull ResourceLocation getStillTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_still");
            }

            @Override
            public @NotNull ResourceLocation getFlowingTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_flow");
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_overlay");
            }

            @Override
            public int getTintColor() {
                return PathogenColors.CONTAMINATED_WATER;
            }
        }, PathogenFluidTypes.CONTAMINATED_WATER.get());
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(PathogenFluids.CONTAMINATED_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                PathogenFluids.FLOWING_CONTAMINATED_WATER.get(),
                RenderType.translucent()
            );
        });
    }
}
