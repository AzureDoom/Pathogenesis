package com.azure.pathogenesis.client.renderer.block;

import com.azure.pathogenesis.block.SporePlantBlock;
import com.azure.pathogenesis.block.SporePodLayout;
import com.azure.pathogenesis.blockentity.SporePlantBlockEntity;
import com.azure.pathogenesis.client.PathogenResources;
import com.azure.pathogenesis.client.animator.SporePlantAnimator;
import com.azure.pathogenesis.client.renderer.layer.EmitterTrackingLayer;
import com.mojang.math.Axis;
import mod.azure.azurelib.common.render.block.AzBlockEntityRenderer;
import mod.azure.azurelib.common.render.block.AzBlockEntityRendererConfig;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

@SuppressWarnings("unused")
public class SporePlantRenderer extends AzBlockEntityRenderer<SporePlantBlockEntity> {

    public SporePlantRenderer(BlockEntityRendererProvider.Context context) {
        this(new EmitterTrackingLayer());
    }

    private SporePlantRenderer(EmitterTrackingLayer emitterLayer) {
        super(
            AzBlockEntityRendererConfig.<SporePlantBlockEntity>builder(
                PathogenResources.SPOREPODS.geo(),
                PathogenResources.SPOREPODS.texture()
            )
                .setAnimatorProvider(SporePlantAnimator::new)
                .addRenderLayer(emitterLayer)
                .setRenderEntry(contextPipeline -> {
                    var blockEntity = contextPipeline.animatable();
                    var poseStack = contextPipeline.poseStack();
                    var partialTick = contextPipeline.partialTick();
                    var bufferSource = contextPipeline.multiBufferSource();
                    var packedLight = contextPipeline.packedLight();
                    var packedOverlay = contextPipeline.packedOverlay();

                    if (blockEntity.getLevel() == null) {
                        return contextPipeline;
                    }
                    var state = blockEntity.getBlockState();
                    if (
                        !(state.getBlock() instanceof SporePlantBlock)
                            || state.getValue(SporePlantBlock.AGE) != SporePlantBlock.MAX_AGE
                    ) {
                        return contextPipeline;
                    }
                    emitterLayer.beginFrame(poseStack.last().pose());

                    var pods = SporePodLayout.of(blockEntity.getBlockPos());
                    for (var i = 0; i < pods.size(); i++) {
                        var pod = pods.get(i);
                        emitterLayer.setInstance(i);
                        poseStack.pushPose();
                        poseStack.translate(pod.x(), 0.0D, pod.z());
                        poseStack.mulPose(Axis.YP.rotationDegrees(pod.yaw()));
                        poseStack.scale(pod.scale(), pod.scale(), pod.scale());
                        poseStack.translate(-0.5D, 0.0D, -0.5D);
                        poseStack.popPose();
                    }
                    return contextPipeline;
                })
                .build()
        );
    }
}
