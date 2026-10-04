package com.azure.pathogenesis.exposure;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.registry.PathogenDamageTypes;
import com.azure.pathogenesis.registry.PathogenSounds;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class PathogenExposureEffects {

    private PathogenExposureEffects() {}

    public static void apply(LivingEntity entity, PathogenExposure exposure) {
        switch (exposure.tier()) {
            case NONE -> {}
            case LOW -> effect(entity, MobEffects.CONFUSION, 0);
            case MODERATE -> {
                effect(entity, MobEffects.CONFUSION, 0);
                effect(entity, MobEffects.WEAKNESS, 0);
                effect(entity, MobEffects.HUNGER, 0);
            }
            case HIGH -> {
                effect(entity, MobEffects.CONFUSION, 0);
                effect(entity, MobEffects.WEAKNESS, 1);
                effect(entity, MobEffects.MOVEMENT_SLOWDOWN, 1);
                effect(entity, MobEffects.POISON, 0);
            }
            case EXTREME -> {
                var kills = Pathogenesis.getConfig().lifecycleConfigs.extremeExposureKills;
                effect(entity, MobEffects.WITHER, kills ? 0 : 1);
                effect(entity, MobEffects.WEAKNESS, 2);
                effect(entity, MobEffects.CONFUSION, 0);
                effect(entity, MobEffects.MOVEMENT_SLOWDOWN, 1);
            }
        }
    }

    public static void tickLethal(LivingEntity entity, PathogenExposure exposure) {
        var remaining = exposure.tickLethal();
        if (remaining < 0) {
            return;
        }
        if (remaining == 0) {
            if (Pathogenesis.getConfig().lifecycleConfigs.extremeExposureKills) {
                entity.hurt(PathogenDamageTypes.source(entity.level(), PathogenDamageTypes.PATHOGEN), Float.MAX_VALUE);
            }
            return;
        }
        if (entity instanceof ServerPlayer player && remaining % 20 == 0) {
            var urgency = 1.0F - remaining / (float) 300;
            player.playNotifySound(
                PathogenSounds.HOST_HEARTBEAT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F + urgency * 0.8F
            );
        }
    }

    private static void effect(LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, 100, amplifier, false, true, true));
    }
}
