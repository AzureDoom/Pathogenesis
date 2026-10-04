package com.azure.pathogenesis.infection;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PathogenTreatments {

    private PathogenTreatments() {}

    public static void onItemConsumed(LivingEntity entity, ItemStack stack) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        var state = PathogenHosts.get(entity);
        if (state == null) {
            return;
        }
        if (stack.is(Items.HONEY_BOTTLE)) {
            honey(level, entity, state);
        } else if (stack.is(Items.GOLDEN_APPLE)) {
            apple(level, entity, state, false);
        } else if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            apple(level, entity, state, true);
        }
    }

    private static void honey(ServerLevel level, LivingEntity entity, HostState state) {
        var applied = false;
        var infection = state.infection();
        if (infection != null && !infection.isTreating() && infection.stage() == NeomorphInfectionStage.EXPOSED) {
            NeomorphInfections.purge(level, entity, state);
            applied = true;
        }
        var exposure = state.exposure();
        if (exposure != null && !exposure.isEmpty()) {
            exposure.reduce(20);
            applied = true;
        }
        if (applied && entity instanceof Player player) {
            player.getCooldowns().addCooldown(Items.HONEY_BOTTLE, 600);
        }
    }

    private static void apple(ServerLevel level, LivingEntity entity, HostState state, boolean enchanted) {
        var infection = state.infection();
        if (infection == null || infection.isTreating() || !entity.hasEffect(MobEffects.WEAKNESS)) {
            return;
        }
        var rate = switch (infection.stage()) {
            case EXPOSED, INCUBATING -> enchanted ? 3 : 2;
            case SYMPTOMATIC -> enchanted ? 3 : isCauterizing(entity) ? 2 : 0;
            case TERMINAL -> 0;
        };
        if (rate == 0) {
            return;
        }
        infection.beginTreatment(rate);
        level.playSound(
            null,
            entity.blockPosition(),
            SoundEvents.ZOMBIE_VILLAGER_CURE,
            SoundSource.PLAYERS,
            1.0F,
            1.0F
        );
    }

    private static boolean isCauterizing(LivingEntity entity) {
        return entity.isOnFire() && entity.hasEffect(MobEffects.FIRE_RESISTANCE);
    }
}
