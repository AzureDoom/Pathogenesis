package com.azure.pathogenesis.client.renderer.block;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.block.SporePlantBlock;
import com.azure.pathogenesis.block.SporePodLayout;
import com.azure.pathogenesis.blockentity.SporePlantBlockEntity;
import com.azure.pathogenesis.client.animator.SporePlantAnimator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mod.azure.azurelib.common.model.AzBone;
import mod.azure.azurelib.common.render.AzRendererPipelineContext;
import mod.azure.azurelib.common.render.block.AzBlockEntityRenderer;
import mod.azure.azurelib.common.render.block.AzBlockEntityRendererConfig;
import mod.azure.azurelib.common.render.layer.AzRenderLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SporePlantRenderer extends AzBlockEntityRenderer<SporePlantBlockEntity> {

    private static final ResourceLocation GEO = Pathogenesis.id("geo/block/sporepods.geo.json");

    private static final ResourceLocation TEXTURE = Pathogenesis.id("textures/block/sporepods.png");

    private final EmitterTrackingLayer emitterLayer;

    public SporePlantRenderer(BlockEntityRendererProvider.Context context) {
        this(new EmitterTrackingLayer());
    }

    private SporePlantRenderer(EmitterTrackingLayer emitterLayer) {
        super(
            AzBlockEntityRendererConfig.<SporePlantBlockEntity>builder(GEO, TEXTURE)
                .setAnimatorProvider(SporePlantAnimator::new)
                .addRenderLayer(emitterLayer)
                .build()
        );
        this.emitterLayer = emitterLayer;
    }

    @Override
    public void render(
        @NotNull SporePlantBlockEntity blockEntity,
        float partialTick,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        var state = blockEntity.getBlockState();
        if (
            !(state.getBlock() instanceof SporePlantBlock)
                || state.getValue(SporePlantBlock.AGE) != SporePlantBlock.MAX_AGE
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

    private static final class EmitterTrackingLayer implements AzRenderLayer<Long, SporePlantBlockEntity> {

        private static final String PREFIX = "particle_";

        private final Matrix4f blockSpaceInverse = new Matrix4f();

        private int instance;

        void beginFrame(Matrix4f blockSpace) {
            blockSpaceInverse.set(blockSpace).invert();
        }

        void setInstance(int instance) {
            this.instance = instance;
        }

        @Override
        public void preRender(AzRendererPipelineContext<Long, SporePlantBlockEntity> context) {}

        @Override
        public void render(AzRendererPipelineContext<Long, SporePlantBlockEntity> context) {}

        @Override
        public void renderForBone(AzRendererPipelineContext<Long, SporePlantBlockEntity> context, AzBone bone) {
            if (!bone.getName().startsWith(PREFIX)) {
                return;
            }
            var pivot = new Vector3f(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
            new Matrix4f(blockSpaceInverse).mul(context.poseStack().last().pose()).transformPosition(pivot);
            context.animatable().setEmitter(bone.getName() + "#" + instance, new Vec3(pivot.x, pivot.y, pivot.z));
        }
    }
}
