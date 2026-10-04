package com.azure.pathogenesis.mixin;

import com.azure.pathogenesis.item.AmpuleDamageTracker;
import com.azure.pathogenesis.item.AmpuleHazards;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin implements AmpuleDamageTracker {

    @Unique
    @Nullable
    private DamageSource pathogenesis$lastDamage;

    @Unique
    private float pathogenesis$fallBefore;

    @Inject(method = "hurt", at = @At("HEAD"))
    private void pathogenesis$recordDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        pathogenesis$lastDamage = source;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void pathogenesis$tickHead(CallbackInfo ci) {
        pathogenesis$fallBefore = ((ItemEntity) (Object) this).fallDistance;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void pathogenesis$tickTail(CallbackInfo ci) {
        var self = (ItemEntity) (Object) this;
        if (
            pathogenesis$fallBefore >= 1.0F && self.onGround() && !self.isRemoved() && !self.isInWater()
                && self.level() instanceof ServerLevel level
        ) {
            AmpuleHazards.onLanded(level, self, pathogenesis$fallBefore);
        }
    }

    @Override
    public @Nullable DamageSource pathogenesis$lastDamage() {
        return pathogenesis$lastDamage;
    }
}
