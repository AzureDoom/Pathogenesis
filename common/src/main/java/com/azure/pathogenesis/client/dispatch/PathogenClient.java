package com.azure.pathogenesis.client.dispatch;

import com.azure.pathogenesis.client.renderer.BloodbursterRenderer;
import com.azure.pathogenesis.client.renderer.NeomorphRenderer;
import com.azure.pathogenesis.client.renderer.NeophyteRenderer;
import com.azure.pathogenesis.client.renderer.NoopRenderer;
import com.azure.pathogenesis.client.renderer.block.PathogenSourceItemRenderer;
import com.azure.pathogenesis.client.renderer.block.PathogenSourceRenderer;
import com.azure.pathogenesis.client.renderer.block.SporePlantItemRenderer;
import com.azure.pathogenesis.client.renderer.block.SporePlantRenderer;
import com.azure.pathogenesis.client.renderer.item.SealedAmpuleRenderer;
import com.azure.pathogenesis.registry.PathogenBlockEntities;
import com.azure.pathogenesis.registry.PathogenEntities;
import com.azure.pathogenesis.registry.PathogenItems;
import mod.azure.azurelib.common.render.item.AzItemRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class PathogenClient {

    @FunctionalInterface
    public interface RendererSink {

        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    @FunctionalInterface
    public interface BlockEntityRendererSink {

        <T extends BlockEntity> void register(
            BlockEntityType<? extends T> type,
            BlockEntityRendererProvider<T> provider
        );
    }

    private PathogenClient() {}

    public static void registerBlockEntityRenderers(BlockEntityRendererSink sink) {
        sink.register(PathogenBlockEntities.SPORE_PLANT.get(), SporePlantRenderer::new);
        sink.register(PathogenBlockEntities.PATHOGEN_SOURCE.get(), PathogenSourceRenderer::new);
    }

    public static void registerRenderers(RendererSink sink) {
        sink.register(PathogenEntities.SPORE_CLOUD.get(), NoopRenderer::new);
        sink.register(PathogenEntities.BLOODBURSTER.get(), BloodbursterRenderer::new);
        sink.register(PathogenEntities.NEOPHYTE.get(), NeophyteRenderer::new);
        sink.register(PathogenEntities.NEOMORPH.get(), NeomorphRenderer::new);
        sink.register(PathogenEntities.THROWN_PATHOGEN_AMPULE.get(), ThrownItemRenderer::new);
    }

    public static void registerItemRenderers() {
        AzItemRendererRegistry.register(PathogenItems.SPORE_PLANT.get(), SporePlantItemRenderer::new);
        AzItemRendererRegistry.register(PathogenItems.PATHOGEN_SOURCE.get(), PathogenSourceItemRenderer::new);
        AzItemRendererRegistry.register(PathogenItems.SEALED_PATHOGEN_AMPULE.get(), SealedAmpuleRenderer::new);
    }
}
