package com.azure.pathogenesis.client.renderer;

import com.azure.pathogenesis.client.PathogenResources;
import com.azure.pathogenesis.client.animator.PathogenEntityAnimator;
import com.azure.pathogenesis.entity.BloodbursterEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class BloodbursterRenderer extends AzEntityRenderer<BloodbursterEntity> {

    public BloodbursterRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<BloodbursterEntity>builder(
                PathogenResources.BLOODBURSTER.geo(),
                PathogenResources.BLOODBURSTER.texture()
            )
                .setAnimatorProvider(PathogenEntityAnimator::new)
                .build(),
            context
        );
    }
}
