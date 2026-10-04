package com.azure.pathogenesis.client.animator;

import com.azure.pathogenesis.blockentity.SporePlantBlockEntity;
import com.azure.pathogenesis.client.PathogenResources;
import mod.azure.azurelib.common.animation.AzAnimatorConfig;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzBlockAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SporePlantAnimator extends AzBlockAnimator<SporePlantBlockEntity> {

    public SporePlantAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<SporePlantBlockEntity> container) {
        container.add(AzAnimationController.builder(this, SporePlantBlockEntity.CONTROLLER).build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(SporePlantBlockEntity blockEntity) {
        return PathogenResources.SPOREPODS.animation();
    }
}
