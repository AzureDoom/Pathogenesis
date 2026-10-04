package com.azure.pathogenesis.client.renderer.block;

import com.azure.pathogenesis.client.PathogenResources;
import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererConfig;

public class SporePlantItemRenderer extends AzItemRenderer {

    public SporePlantItemRenderer() {
        super(
            AzItemRendererConfig.builder(PathogenResources.SPOREPODS.geo(), PathogenResources.SPOREPODS.texture())
                .build()
        );
    }
}
