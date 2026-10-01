package com.azure.pathogenesis.exposure;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.registry.PathogenDamageTypes;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class PathogenExposureEffects {

    public static final int LOW = 20;

    public static final int MODERATE = 55;

    public static final int HIGH = 110;

    public static final int EXTREME = 180;

    private static final int DURATION = 100;

    private PathogenExposureEffects() {}

    public static void apply(LivingEntity entity, PathogenExposure exposure) {
        var dose = exposure.exposure();
        if (dose >= EXTREME) {
            if (Pathogenesis.getConfig().lifecycleConfigs.extremeExposureKills) {
                entity.hurt(PathogenDamageTypes.source(entity.level(), PathogenDamageTypes.PATHOGEN), Float.MAX_VALUE);
            } else {
                effect(entity, MobEffects.WITHER, 1);
                effect(entity, MobEffects.WEAKNESS, 2);
            }
            return;
        }
        if (dose >= HIGH) {
            effect(entity, MobEffects.CONFUSION, 0);
            effect(entity, MobEffects.WEAKNESS, 1);
            effect(entity, MobEffects.MOVEMENT_SLOWDOWN, 1);
            effect(entity, MobEffects.POISON, 0);
        } else if (dose >= MODERATE) {
            effect(entity, MobEffects.CONFUSION, 0);
            effect(entity, MobEffects.WEAKNESS, 0);
            effect(entity, MobEffects.HUNGER, 0);
        } else if (dose >= LOW) {
            effect(entity, MobEffects.CONFUSION, 0);
        }
    }

    private static void effect(LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, DURATION, amplifier, false, true, true));
    }
}
