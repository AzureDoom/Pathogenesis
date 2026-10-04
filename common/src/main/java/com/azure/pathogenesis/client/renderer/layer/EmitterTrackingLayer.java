package com.azure.pathogenesis.client.renderer.layer;

import com.azure.pathogenesis.blockentity.SporePlantBlockEntity;
import mod.azure.azurelib.common.model.AzBone;
import mod.azure.azurelib.common.render.AzRendererPipelineContext;
import mod.azure.azurelib.common.render.layer.AzRenderLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class EmitterTrackingLayer implements AzRenderLayer<Long, SporePlantBlockEntity> {

    private static final String PREFIX = "particle_";

    private final Matrix4f blockSpaceInverse = new Matrix4f();

    private int instance;

    public void beginFrame(Matrix4f blockSpace) {
        blockSpaceInverse.set(blockSpace).invert();
    }

    public void setInstance(int instance) {
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
