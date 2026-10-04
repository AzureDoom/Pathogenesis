package com.azure.pathogenesis.item;

import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.Nullable;

public interface AmpuleDamageTracker {

    @Nullable
    DamageSource pathogenesis$lastDamage();
}
