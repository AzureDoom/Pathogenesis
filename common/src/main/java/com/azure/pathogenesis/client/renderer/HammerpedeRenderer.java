package com.azure.pathogenesis.client.renderer;

import com.azure.pathogenesis.client.PathogenResources;
import com.azure.pathogenesis.client.animator.PathogenEntityAnimator;
import com.azure.pathogenesis.entity.HammerpedeEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.common.render.lod.AzLodConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class HammerpedeRenderer extends AzEntityRenderer<HammerpedeEntity> {

    public HammerpedeRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<HammerpedeEntity>builder(
                PathogenResources.HAMMERPEDE.geo(),
                PathogenResources.HAMMERPEDE.texture()
            )
                .setAnimatorProvider(PathogenEntityAnimator::new)
                .withLodConfig(AzLodConfig.DEFAULT)
                .build(),
            context
        );
    }
}
