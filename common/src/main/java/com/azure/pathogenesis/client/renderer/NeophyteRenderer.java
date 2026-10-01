package com.azure.pathogenesis.client.renderer;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.client.animator.PathogenAnimator;
import com.azure.pathogenesis.entity.NeophyteEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class NeophyteRenderer extends AzEntityRenderer<NeophyteEntity> {

    private static final ResourceLocation GEO = Pathogenesis.id("geo/entity/neophyte.geo.json");

    private static final ResourceLocation TEX = Pathogenesis.id("textures/entity/neophyte.png");

    public NeophyteRenderer(EntityRendererProvider.Context context) {
        super(
            AzEntityRendererConfig.<NeophyteEntity>builder(GEO, TEX)
                .setAnimatorProvider(() -> new PathogenAnimator<>("neophyte"))
                .build(),
            context
        );
        this.shadowRadius = 0.45F;
    }
}
