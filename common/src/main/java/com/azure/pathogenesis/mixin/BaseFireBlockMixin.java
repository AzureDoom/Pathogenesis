package com.azure.pathogenesis.mixin;

import com.azure.pathogenesis.contamination.PathogenSterilization;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin {

    @Inject(method = "onPlace", at = @At("TAIL"))
    private void pathogenesis$onPlace(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState oldState,
        boolean isMoving,
        CallbackInfo ci
    ) {
        if (level instanceof ServerLevel serverLevel && !oldState.is(state.getBlock())) {
            PathogenSterilization.onFire(serverLevel, pos);
        }
    }
}
