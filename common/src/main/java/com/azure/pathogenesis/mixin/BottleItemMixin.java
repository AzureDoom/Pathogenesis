package com.azure.pathogenesis.mixin;

import com.azure.pathogenesis.registry.PathogenFluids;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BottleItem.class)
public abstract class BottleItemMixin {

    @WrapOperation(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"
        )
    )
    private boolean pathogenesis$refuseContaminatedWater(
        FluidState fluidState,
        TagKey<Fluid> tag,
        Operation<Boolean> original
    ) {
        if (fluidState.getType().isSame(PathogenFluids.CONTAMINATED_WATER.get())) {
            return false;
        }
        return original.call(fluidState, tag);
    }
}
