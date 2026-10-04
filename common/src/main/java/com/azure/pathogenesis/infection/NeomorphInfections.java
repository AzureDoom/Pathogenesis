package com.azure.pathogenesis.infection;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.contamination.CarcassSites;
import com.azure.pathogenesis.exposure.ExposureType;
import com.azure.pathogenesis.exposure.PathogenExposure;
import com.azure.pathogenesis.registry.PathogenDamageTypes;
import com.azure.pathogenesis.registry.PathogenEntities;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import com.azure.pathogenesis.registry.PathogenTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

public final class NeomorphInfections {

    public static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.45F, 0.02F, 0.02F), 1.0F);

    private NeomorphInfections() {}

    public static boolean isValidHost(LivingEntity entity) {
        if (!entity.isAlive() || entity.isBaby() && entity.getBbHeight() < 0.4F) {
            return false;
        }
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        var type = entity.getType();
        return type.is(PathogenTags.Entities.VALID_HOSTS)
            && !type.is(PathogenTags.Entities.SPORE_IMMUNE)
            && !type.is(PathogenTags.Entities.PATHOGEN_IMMUNE);
    }

    public static boolean isInfected(LivingEntity entity) {
        var state = PathogenHosts.get(entity);
        return state != null && state.infection() != null;
    }

    public static void exposeToSpores(LivingEntity entity, @Nullable UUID zoneId, BlockPos cloudPos) {
        if (!isValidHost(entity)) {
            return;
        }
        if (isInfected(entity)) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            PathogenTriggers.trigger(player, PathogenTriggers.SPORE_EXPOSED);
        }
        var helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty() && helmet.is(PathogenTags.Items.SPORE_FILTERS)) {
            helmet.hurtAndBreak(1, entity, EquipmentSlot.HEAD);
            return;
        }
        PathogenHosts.getOrCreate(entity)
            .getOrCreateExposure()
            .add(5, ExposureType.SPORE, entity.level().getGameTime());
        infect(entity, new InfectionSite(cloudPos.immutable(), zoneId));
    }

    public static boolean infect(LivingEntity entity, InfectionSite site) {
        if (isInfected(entity)) {
            return false;
        }
        var random = entity.getRandom();
        var base = Pathogenesis.getConfig().lifecycleConfigs.neomorphIncubationTime;
        var total = Math.max(200, (int) (base * (0.85F + random.nextFloat() * 0.30F)));
        PathogenHosts.getOrCreate(entity).setInfection(new NeomorphInfection(total, site));
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        entity.level()
            .playSound(null, entity.blockPosition(), PathogenSounds.HOST_COUGH.get(), SoundSource.HOSTILE, 0.6F, 1.1F);
        return true;
    }

    public static float progressionRate(@Nullable PathogenExposure exposure) {
        if (exposure == null) {
            return 1.0F;
        }
        var config = Pathogenesis.getConfig().lifecycleConfigs;
        return switch (exposure.tier()) {
            case HIGH -> (float) config.highExposureProgression;
            case EXTREME -> (float) config.extremeExposureProgression;
            default -> 1.0F;
        };
    }

    static void tickInfection(ServerLevel level, LivingEntity entity, HostState state, NeomorphInfection infection) {
        if (infection.tick(progressionRate(state.exposure())) && !infection.isTreating()) {
            onStageChanged(entity, infection.stage());
        }
        if (infection.isCured()) {
            cure(level, entity, state);
            return;
        }
        var random = entity.getRandom();
        if (infection.isTreating()) {
            tickTreatment(entity);
        }
        switch (infection.stage()) {
            case EXPOSED -> {}
            case INCUBATING -> tickIncubating(level, entity, random);
            case SYMPTOMATIC -> tickSymptomatic(level, entity, random);
            case TERMINAL -> burst(level, entity, state, infection, true);
        }
    }

    public static void cure(ServerLevel level, LivingEntity entity, HostState state) {
        state.setInfection(null);
        level.playSound(
            null,
            entity.blockPosition(),
            SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
            SoundSource.PLAYERS,
            0.8F,
            1.2F
        );
    }

    public static void purge(ServerLevel level, LivingEntity entity, HostState state) {
        state.setInfection(null);
        cough(level, entity, 1.0F);
    }

    private static void onStageChanged(LivingEntity entity, NeomorphInfectionStage stage) {
        if (stage == NeomorphInfectionStage.INCUBATING && entity instanceof ServerPlayer player) {
            PathogenTriggers.trigger(player, PathogenTriggers.INFECTED);
        }
    }

    private static void tickTreatment(LivingEntity entity) {
        if (entity.tickCount % 60 == 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 80, 1, false, false, true));
        }
    }

    private static void tickIncubating(ServerLevel level, LivingEntity entity, RandomSource random) {
        if (random.nextInt(600) == 0) {
            cough(level, entity, 0.7F);
        }
        if (random.nextInt(900) == 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, false, true));
        }
        if (entity instanceof ServerPlayer player && random.nextInt(500) == 0) {
            player.playNotifySound(PathogenSounds.HOST_HEARTBEAT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    private static void tickSymptomatic(ServerLevel level, LivingEntity entity, RandomSource random) {
        if (random.nextInt(120) == 0) {
            cough(level, entity, 1.0F);
            level.sendParticles(
                BLOOD,
                entity.getX(),
                entity.getEyeY() - 0.2D,
                entity.getZ(),
                8,
                0.15D,
                0.1D,
                0.15D,
                0.02D
            );
        }
        if (entity.tickCount % 60 == 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, false, true));
        }
        if (random.nextInt(240) == 0 && entity.getHealth() > 3.0F) {
            entity.hurt(PathogenDamageTypes.source(level, PathogenDamageTypes.PATHOGEN), 1.0F);
        }
        if (entity instanceof ServerPlayer player && random.nextInt(160) == 0) {
            player.playNotifySound(PathogenSounds.HOST_HEARTBEAT.get(), SoundSource.PLAYERS, 1.0F, 1.3F);
        }
    }

    private static void cough(ServerLevel level, LivingEntity entity, float volume) {
        level.playSound(
            null,
            entity.blockPosition(),
            PathogenSounds.HOST_COUGH.get(),
            SoundSource.HOSTILE,
            volume,
            0.9F + entity.getRandom().nextFloat() * 0.2F
        );
    }

    public static void burst(
        ServerLevel level,
        LivingEntity entity,
        HostState state,
        NeomorphInfection infection,
        boolean damageHost
    ) {
        state.setInfection(null);

        var bloodBurster = PathogenEntities.BLOODBURSTER.get().create(level);
        if (bloodBurster != null) {
            bloodBurster.moveTo(
                entity.getX(),
                entity.getY() + entity.getBbHeight() * 0.5D,
                entity.getZ(),
                level.random.nextFloat() * 360.0F,
                0.0F
            );
            bloodBurster.setOriginZone(infection.site().zoneId());
            bloodBurster.markEmerged();
            level.addFreshEntity(bloodBurster);
        }

        var y = entity.getY() + entity.getBbHeight() * 0.6D;
        level.sendParticles(BLOOD, entity.getX(), y, entity.getZ(), 40, 0.3D, 0.3D, 0.3D, 0.15D);
        level.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
            entity.getX(),
            y,
            entity.getZ(),
            30,
            0.25D,
            0.25D,
            0.25D,
            0.2D
        );
        level.playSound(null, entity.blockPosition(), PathogenSounds.BLOODBURST.get(), SoundSource.HOSTILE, 1.3F, 1.0F);
        CarcassSites.onBurst(level, entity, infection.site().zoneId());
        PathogenTriggers.triggerNearby(
            level,
            entity.position(),
            24D,
            PathogenTriggers.BLOODBURSTER_WITNESSED
        );

        if (!damageHost) {
            return;
        }
        var source = PathogenDamageTypes.source(level, PathogenDamageTypes.BLOODBURST);
        if (entity instanceof Player && !Pathogenesis.getConfig().lifecycleConfigs.playerBurstKills) {
            entity.hurt(source, Math.max(0.0F, entity.getHealth() - 1.0F));
        } else {
            entity.hurt(source, Float.MAX_VALUE);
        }
    }

    public static void onHostDeath(LivingEntity entity) {
        var state = PathogenHosts.get(entity);
        NeomorphInfection infection = state == null ? null : state.infection();
        var bursting = infection != null && infection.stage().burstsOnDeath();
        if (!bursting && entity.level() instanceof ServerLevel level) {
            CarcassSites.onDeath(level, entity, state);
        }
        if (state == null) {
            return;
        }
        if (bursting && entity.level() instanceof ServerLevel level) {
            burst(level, entity, state, infection, false);
        }
        PathogenHosts.remove(entity);
    }
}
