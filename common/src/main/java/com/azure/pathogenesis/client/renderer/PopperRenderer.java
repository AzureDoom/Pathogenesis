package com.azure.pathogenesis.client.renderer;

import com.azure.pathogenesis.client.PathogenResources;
import com.azure.pathogenesis.client.animator.PathogenEntityAnimator;
import com.azure.pathogenesis.entity.PopperEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.common.render.lod.AzLodConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class PopperRenderer extends AzEntityRenderer<PopperEntity> {

    public PopperRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<PopperEntity>builder(
                PathogenResources.PATHOGEN_POPPER.geo(),
                PathogenResources.PATHOGEN_POPPER.texture()
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
    }
}
