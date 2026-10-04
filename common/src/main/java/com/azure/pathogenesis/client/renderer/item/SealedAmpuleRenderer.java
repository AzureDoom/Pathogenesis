package com.azure.pathogenesis.client.renderer.item;

import com.azure.pathogenesis.client.PathogenResources;
import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererConfig;

public class SealedAmpuleRenderer extends AzItemRenderer {

    public SealedAmpuleRenderer() {
        super(
            AzItemRendererConfig.builder(
                PathogenResources.SEALED_PATHOGEN_AMPULE.geo(),
                PathogenResources.SEALED_PATHOGEN_AMPULE.texture()
            )
                .build()
        );
    }
}
