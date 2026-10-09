package com.azure.pathogenesis.client.renderer.block;

import com.azure.pathogenesis.block.PathogenSourceBlock;
import com.azure.pathogenesis.client.PathogenResources;
import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererConfig;

public class PathogenSourceItemRenderer extends AzItemRenderer {

    public PathogenSourceItemRenderer() {
        super(
            AzItemRendererConfig.builder(
                stack -> PathogenResources.SOURCE_GEO,
                stack -> PathogenResources.sourceTexture(PathogenSourceBlock.itemDisplayState(stack))
            )
                .build()
        );
    }
}
