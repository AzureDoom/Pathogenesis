package com.azure.pathogenesis.client.animator;

import com.azure.pathogenesis.blockentity.PathogenSourceBlockEntity;
import com.azure.pathogenesis.client.PathogenResources;
import mod.azure.azurelib.common.animation.AzAnimatorConfig;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzItemAnimator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PathogenSourceItemAnimator extends AzItemAnimator {

    public PathogenSourceItemAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<ItemStack> container) {
        container.add(AzAnimationController.builder(this, PathogenSourceBlockEntity.CONTROLLER).build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(ItemStack stack) {
        return PathogenResources.SOURCE_ANIMATION;
    }
}
