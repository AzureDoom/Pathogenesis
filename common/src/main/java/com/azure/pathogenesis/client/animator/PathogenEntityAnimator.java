package com.azure.pathogenesis.client.animator;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.entity.anim.PathogenAnimationDispatcher;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class PathogenEntityAnimator<T extends Entity> extends AzEntityAnimator<T> {

    private final ResourceLocation animation;

    public PathogenEntityAnimator(String entityName) {
        this.animation = Pathogenesis.id("animations/entity/" + entityName + ".animation.json");
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<T> container) {
        container.add(AzAnimationController.builder(this, PathogenAnimationDispatcher.CONTROLLER).build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(T entity) {
        return animation;
    }
}
