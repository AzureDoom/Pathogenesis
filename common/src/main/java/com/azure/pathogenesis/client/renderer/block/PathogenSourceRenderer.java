package com.azure.pathogenesis.client.renderer.block;

import com.azure.pathogenesis.block.PathogenSourceBlock;
import com.azure.pathogenesis.blockentity.PathogenSourceBlockEntity;
import com.azure.pathogenesis.client.PathogenResources;
import com.azure.pathogenesis.client.animator.PathogenSourceAnimator;
import mod.azure.azurelib.common.render.block.AzBlockEntityRenderer;
import mod.azure.azurelib.common.render.block.AzBlockEntityRendererConfig;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

@SuppressWarnings("unused")
public class PathogenSourceRenderer extends AzBlockEntityRenderer<PathogenSourceBlockEntity> {

    public PathogenSourceRenderer(BlockEntityRendererProvider.Context context) {
        super(
            AzBlockEntityRendererConfig.<PathogenSourceBlockEntity>builder(
                blockEntity -> PathogenResources.SOURCE_GEO,
                blockEntity -> PathogenResources.sourceTexture(
                    blockEntity.getBlockState().getValue(PathogenSourceBlock.CONTAINMENT)
                )
            )
                .setAnimatorProvider(PathogenSourceAnimator::new)
                .setRenderEntry(contextPipeline -> {
                    contextPipeline.animatable().syncAnimation();

                    return contextPipeline;
                })
                .build()
        );
    }
}
