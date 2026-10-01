package com.azure.pathogenesis.mixin;

import com.azure.pathogenesis.infection.PathogenHosts;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
    private void pathogenesis$tick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        PathogenHosts.tick((ServerLevel) (Object) this);
    }
}
