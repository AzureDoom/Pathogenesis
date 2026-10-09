package com.azure.pathogenesis.client.renderer.block;

import com.azure.pathogenesis.block.SporePlantBlock;
import com.azure.pathogenesis.block.SporePodLayout;
import com.azure.pathogenesis.blockentity.SporePlantBlockEntity;
import com.azure.pathogenesis.client.PathogenResources;
import com.azure.pathogenesis.client.animator.SporePlantAnimator;
import com.azure.pathogenesis.client.renderer.layer.EmitterTrackingLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mod.azure.azurelib.common.render.block.AzBlockEntityRenderer;
import mod.azure.azurelib.common.render.block.AzBlockEntityRendererConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class SporePlantRenderer extends AzBlockEntityRenderer<SporePlantBlockEntity> {

    private final EmitterTrackingLayer emitterLayer;

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
                .build()
        );
        this.emitterLayer = emitterLayer;
    }

    @Override
    public void render(
        SporePlantBlockEntity blockEntity,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        var state = blockEntity.getBlockState();
        if (
            !(state.getBlock() instanceof SporePlantBlock) || state.getValue(
                SporePlantBlock.AGE
            ) != SporePlantBlock.MAX_AGE
        ) {
            return;
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
            super.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }
}
