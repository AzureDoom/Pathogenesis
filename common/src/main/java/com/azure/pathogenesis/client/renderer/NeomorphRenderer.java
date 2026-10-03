package com.azure.pathogenesis.client.renderer;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.client.animator.PathogenEntityAnimator;
import com.azure.pathogenesis.entity.NeomorphEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class NeomorphRenderer extends AzEntityRenderer<NeomorphEntity> {

    private static final ResourceLocation GEO = Pathogenesis.id("geo/entity/neomorph.geo.json");

    private static final ResourceLocation TEX = Pathogenesis.id("textures/entity/neomorph.png");

    public NeomorphRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<NeomorphEntity>builder(GEO, TEX)
                .setAnimatorProvider(() -> new PathogenEntityAnimator<>("neomorph"))
                .build(),
            context
        );
        this.shadowRadius = 0.6F;
    }
}
