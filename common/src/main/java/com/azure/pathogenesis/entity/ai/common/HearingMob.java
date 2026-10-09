package com.azure.pathogenesis.entity.ai.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface HearingMob extends SoundListener {

    HearingState<?> hearing();

    @Override
    default void onHeard(BlockPos pos, @Nullable Entity cause) {
        hearing().hear(pos);
    }

    @Override
    @Nullable
    default BlockPos heardPos() {
        return hearing().heardPos();
    }

    @Override
    default long heardTick() {
        return hearing().heardTick();
    }

    @Override
    default void clearHeard() {
        hearing().clear();
    }
}
