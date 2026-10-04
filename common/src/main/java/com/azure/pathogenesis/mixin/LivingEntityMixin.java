package com.azure.pathogenesis.mixin;

import com.azure.pathogenesis.infection.NeomorphInfections;
import com.azure.pathogenesis.infection.PathogenTreatments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void pathogenesis$die(DamageSource damageSource, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide()) {
            NeomorphInfections.onHostDeath(self);
        }
    }

    @WrapOperation(
        method = "completeUsingItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack pathogenesis$completeUsingItem(
        ItemStack stack,
        Level level,
        LivingEntity entity,
        Operation<ItemStack> original
    ) {
        var consumed = stack.copy();
        var result = original.call(stack, level, entity);
        if (!level.isClientSide()) {
            PathogenTreatments.onItemConsumed(entity, consumed);
        }
        return result;
    }
}
