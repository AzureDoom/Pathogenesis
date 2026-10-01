package com.azure.pathogenesis.entity.ai.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface SoundListener {

    void onHeard(BlockPos pos, @Nullable Entity cause);

    @Nullable
    BlockPos heardPos();

    long heardTick();

    void clearHeard();
}
