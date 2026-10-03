package com.azure.pathogenesis.client.animator;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.blockentity.SporePlantBlockEntity;
import mod.azure.azurelib.common.animation.AzAnimatorConfig;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzBlockAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SporePlantAnimator extends AzBlockAnimator<SporePlantBlockEntity> {

    private static final ResourceLocation ANIMATION = Pathogenesis.id("animations/block/sporepods.animation.json");

    public SporePlantAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<SporePlantBlockEntity> container) {
        container.add(AzAnimationController.builder(this, SporePlantBlockEntity.CONTROLLER).build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(SporePlantBlockEntity blockEntity) {
        return ANIMATION;
    }
}
