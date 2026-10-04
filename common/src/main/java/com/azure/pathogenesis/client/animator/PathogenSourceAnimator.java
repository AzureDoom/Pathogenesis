package com.azure.pathogenesis.client.animator;

import com.azure.pathogenesis.blockentity.PathogenSourceBlockEntity;
import com.azure.pathogenesis.client.PathogenResources;
import mod.azure.azurelib.common.animation.AzAnimatorConfig;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzBlockAnimator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class PathogenSourceAnimator extends AzBlockAnimator<PathogenSourceBlockEntity> {

    public PathogenSourceAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<PathogenSourceBlockEntity> container) {
        container.add(AzAnimationController.builder(this, PathogenSourceBlockEntity.CONTROLLER).build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(PathogenSourceBlockEntity blockEntity) {
        return PathogenResources.SOURCE_ANIMATION;
    }
}
