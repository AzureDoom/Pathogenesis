package com.azure.pathogenesis.client.renderer;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.client.animator.PathogenEntityAnimator;
import com.azure.pathogenesis.entity.BloodbursterEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BloodbursterRenderer extends AzEntityRenderer<BloodbursterEntity> {

    private static final ResourceLocation GEO = Pathogenesis.id("geo/entity/neophyte.geo.json");

    private static final ResourceLocation TEX = Pathogenesis.id("textures/entity/bloodburster.png");

    public BloodbursterRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<BloodbursterEntity>builder(GEO, TEX)
                .setAnimatorProvider(() -> new PathogenEntityAnimator<>("bloodburster"))
                .build(),
            context
        );
    }
}
