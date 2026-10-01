package com.azure.pathogenesis.mixin;

import com.azure.pathogenesis.infection.NeomorphInfections;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
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
}
