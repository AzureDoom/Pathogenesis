package com.azure.pathogenesis.client.animator;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.entity.anim.AnimationDriver;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class PathogenAnimator<T extends Entity> extends AzEntityAnimator<T> {

    private final ResourceLocation animations;

    public PathogenAnimator(String entityName) {
        this.animations = Pathogenesis.id("animations/entity/" + entityName + ".animation.json");
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<T> container) {
        container.add(AzAnimationController.builder(this, AnimationDriver.BASE_CONTROLLER).build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(T animatable) {
        return animations;
    }
}
