package com.azure.pathogenesis.client.renderer;

import com.azure.pathogenesis.client.PathogenResources;
import com.azure.pathogenesis.client.animator.PathogenEntityAnimator;
import com.azure.pathogenesis.entity.NeomorphEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.common.render.lod.AzLodConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class NeomorphRenderer extends AzEntityRenderer<NeomorphEntity> {

    public NeomorphRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<NeomorphEntity>builder(
                PathogenResources.NEOMORPH.geo(),
                PathogenResources.NEOMORPH.texture()
            )
                .setAnimatorProvider(PathogenEntityAnimator::new)
                .withLodConfig(
                    AzLodConfig.builder()
                        .boneLod(32, 3)
                        .animLod(48, 2)
                        .build()
                )
                .build(),
            context
        );
        this.shadowRadius = 0.6F;
    }
}
