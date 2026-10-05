package com.azure.pathogenesis.client.animator;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.client.PathogenResources;
import com.azure.pathogenesis.entity.BloodbursterEntity;
import com.azure.pathogenesis.entity.HammerpedeEntity;
import com.azure.pathogenesis.entity.NeomorphEntity;
import com.azure.pathogenesis.entity.NeophyteEntity;
import com.azure.pathogenesis.entity.PopperEntity;
import com.azure.pathogenesis.entity.anim.PathogenAnimationDispatcher;
import mod.azure.azurelib.AzureLib;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class PathogenEntityAnimator<T extends Entity> extends AzEntityAnimator<T> {

    @Override
    public void registerControllers(AzAnimationControllerContainer<T> container) {
        container.add(AzAnimationController.builder(this, PathogenAnimationDispatcher.CONTROLLER).build());
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(T entity) {
        if (entity instanceof BloodbursterEntity) {
            return PathogenResources.BLOODBURSTER.animation();
        }
        if (entity instanceof NeophyteEntity) {
            return PathogenResources.NEOPHYTE.animation();
        }
        if (entity instanceof NeomorphEntity) {
            return PathogenResources.NEOMORPH.animation();
        }
        if (entity instanceof HammerpedeEntity) {
            return PathogenResources.HAMMERPEDE.animation();
        }
        if (entity instanceof PopperEntity) {
            return PathogenResources.PATHOGEN_POPPER.animation();
        }
        Pathogenesis.LOGGER.error("No animation registered for {}", entity.getType());
        return AzureLib.modResource("textures/empty.png");
    }
}
