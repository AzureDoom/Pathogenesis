package com.azure.pathogenesis.client.renderer.block;

import com.azure.pathogenesis.Pathogenesis;
import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererConfig;
import net.minecraft.resources.ResourceLocation;

public class SporePlantItemRenderer extends AzItemRenderer {

    private static final ResourceLocation GEO = Pathogenesis.id("geo/block/sporepods.geo.json");

    private static final ResourceLocation TEXTURE = Pathogenesis.id("textures/block/sporepods.png");

    public SporePlantItemRenderer() {
        super(
            AzItemRendererConfig.builder(GEO, TEXTURE).build()
        );
    }
}
