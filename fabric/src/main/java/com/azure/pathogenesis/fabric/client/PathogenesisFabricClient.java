package com.azure.pathogenesis.fabric.client;

import com.azure.pathogenesis.client.PathogenColors;
import com.azure.pathogenesis.client.dispatch.PathogenClient;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.BlockItem;

public final class PathogenesisFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PathogenClient.registerRenderers(EntityRendererRegistry::register);
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
        ColorProviderRegistry.ITEM.register(
            (stack, tintIndex) -> PathogenColors.ampuleTint(tintIndex),
            PathogenItems.SEALED_PATHOGEN_AMPULE.get()
        );
    }
}
