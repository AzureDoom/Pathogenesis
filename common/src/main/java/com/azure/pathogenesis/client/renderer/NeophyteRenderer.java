package com.azure.pathogenesis.client.renderer;

import com.azure.pathogenesis.client.PathogenResources;
import com.azure.pathogenesis.client.animator.PathogenEntityAnimator;
import com.azure.pathogenesis.entity.NeophyteEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.common.render.lod.AzLodConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class NeophyteRenderer extends AzEntityRenderer<NeophyteEntity> {

    public NeophyteRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<NeophyteEntity>builder(
                PathogenResources.NEOPHYTE.geo(),
                PathogenResources.NEOPHYTE.texture()
            )
                .setAnimatorProvider(PathogenEntityAnimator::new)
                .withLodConfig(AzLodConfig.DEFAULT)
                .build(),
            context
        );
        this.shadowRadius = 0.45F;
    }
}
