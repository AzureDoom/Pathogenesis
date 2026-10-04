package com.azure.pathogenesis.fabric.client;

import com.azure.pathogenesis.client.PathogenColors;
import com.azure.pathogenesis.client.dispatch.PathogenClient;
import com.azure.pathogenesis.client.sound.ContaminationAmbience;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.item.BlockItem;

public final class PathogenesisFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PathogenClient.registerRenderers(EntityRendererRegistry::register);
        PathogenClient.registerBlockEntityRenderers(BlockEntityRenderers::register);
        PathogenClient.registerItemRenderers();
        PathogenBlocks.cutoutBlocks()
            .forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block.get(), RenderType.cutout()));
        BlockRenderLayerMap.INSTANCE.putBlock(PathogenBlocks.CONTAMINATED_GRASS.get(), RenderType.cutoutMipped());
        ColorProviderRegistry.BLOCK.register(
            (state, level, pos, tintIndex) -> PathogenColors.tint(state.getBlock(), tintIndex),
            PathogenColors.tintedBlocks()
        );
        ColorProviderRegistry.ITEM.register(
            (stack, tintIndex) -> PathogenColors.tint(((BlockItem) stack.getItem()).getBlock(), tintIndex),
            PathogenColors.tintedBlocks()
        );
        ClientTickEvents.END_CLIENT_TICK.register(ContaminationAmbience::tick);
        var still = PathogenFluids.CONTAMINATED_WATER.get();
        var flowing = PathogenFluids.FLOWING_CONTAMINATED_WATER.get();
        FluidRenderHandlerRegistry.INSTANCE.register(
            still,
            flowing,
            new SimpleFluidRenderHandler(
                SimpleFluidRenderHandler.WATER_STILL,
                SimpleFluidRenderHandler.WATER_FLOWING,
                SimpleFluidRenderHandler.WATER_OVERLAY,
                PathogenColors.CONTAMINATED_WATER
            )
        );
        BlockRenderLayerMap.INSTANCE.putFluids(RenderType.translucent(), still, flowing);
    }
}
