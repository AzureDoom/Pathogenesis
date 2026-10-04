package com.azure.pathogenesis.entity.ai.fauna;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.infection.HostState;
import com.azure.pathogenesis.infection.NeomorphInfections;
import com.azure.pathogenesis.infection.PathogenHosts;
import com.azure.pathogenesis.mixin.MobAccessor;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class ContaminatedFauna {

    private static final Set<Animal> EQUIPPED = Collections.newSetFromMap(new WeakHashMap<>());

    private ContaminatedFauna() {}

    public static boolean isAffected(LivingEntity entity) {
        return entity instanceof Animal && !(entity instanceof Enemy)
            && !entity.getType().is(PathogenTags.Entities.PATHOGEN_IMMUNE);
    }

    public static FaunaSeverity severity(LivingEntity entity) {
        return severity(PathogenHosts.get(entity));
    }

    public static FaunaSeverity severity(@Nullable HostState state) {
        if (state == null) {
            return FaunaSeverity.CLEAN;
        }
        var severity = FaunaSeverity.CLEAN;
        var exposure = state.exposure();
        if (exposure != null) {
            severity = switch (exposure.tier()) {
                case NONE, LOW -> FaunaSeverity.CLEAN;
                case MODERATE -> FaunaSeverity.MODERATE;
                case HIGH -> FaunaSeverity.HIGH;
                case EXTREME -> FaunaSeverity.EXTREME;
            };
        }
        var infection = state.infection();
        if (infection != null && !infection.isTreating()) {
            severity = severity.max(
                switch (infection.stage()) {
                    case EXPOSED -> FaunaSeverity.CLEAN;
                    case INCUBATING -> FaunaSeverity.MODERATE;
                    case SYMPTOMATIC, TERMINAL -> FaunaSeverity.HIGH;
                }
            );
        }
        return severity;
    }

    public static boolean isClean(LivingEntity entity) {
        return severity(entity) == FaunaSeverity.CLEAN;
    }

    public static void tick(ServerLevel level, Animal animal, HostState state) {
        if (!Pathogenesis.getConfig().faunaConfigs.contaminatedAnimalBehavior) {
            return;
        }
        var severity = severity(state);
        if (severity == FaunaSeverity.CLEAN) {
            return;
        }
        equip(animal);

        if (animal.isInLove()) {
            animal.resetLove();
            level.sendParticles(
                ParticleTypes.SMOKE,
                animal.getX(),
                animal.getEyeY(),
                animal.getZ(),
                4,
                0.2D,
                0.1D,
                0.2D,
                0.01D
            );
        }

        var random = animal.getRandom();
        var interval = Pathogenesis.getConfig().faunaConfigs.animalCoughInterval;
        var coughEvery = switch (severity) {
            case HIGH -> interval / 2;
            case EXTREME -> interval / 5;
            default -> interval;
        };
        if (random.nextInt(Math.max(1, coughEvery)) == 0) {
            cough(level, animal, state);
        }

        if (severity == FaunaSeverity.EXTREME) {
            convulse(level, animal);
        }
    }

    private static void equip(Animal animal) {
        if (!EQUIPPED.add(animal)) {
            return;
        }
        var goals = ((MobAccessor) animal).pathogenesis$getGoalSelector();
        if (animal.getType().is(PathogenTags.Entities.CONTAMINATION_AGGRESSIVE)) {
            goals.addGoal(0, new ContaminationFrenzyGoal(animal));
        } else {
            goals.addGoal(
                0,
                new ContaminationPanicGoal(animal, animal.getType().is(PathogenTags.Entities.DRAWN_TO_CONTAMINATION))
            );
        }
        goals.addGoal(1, new AvoidCleanAnimalsGoal(animal));
        goals.addGoal(2, new ContaminatedWanderGoal(animal));
    }

    private static void cough(ServerLevel level, Animal animal, HostState state) {
        var size = animal.getBbWidth() * animal.getBbHeight();
        var pitch = Mth.clamp(1.5F - size * 0.25F, 0.6F, 1.6F) + (animal.getRandom().nextFloat() - 0.5F) * 0.15F;
        level.playSound(
            null,
            animal.blockPosition(),
            PathogenSounds.HOST_COUGH.get(),
            SoundSource.NEUTRAL,
            0.5F,
            pitch
        );
        var look = animal.getLookAngle();
        var x = animal.getX() + look.x * animal.getBbWidth() * 0.6D;
        var y = animal.getEyeY() - 0.1D;
        var z = animal.getZ() + look.z * animal.getBbWidth() * 0.6D;
        level.sendParticles(PathogenZoneManager.PATHOGEN_DUST, x, y, z, 5, 0.1D, 0.05D, 0.1D, 0.02D);
        var infection = state.infection();
        if (infection != null && infection.stage().burstsOnDeath()) {
            level.sendParticles(NeomorphInfections.BLOOD, x, y, z, 6, 0.1D, 0.05D, 0.1D, 0.02D);
        }
    }

    private static void convulse(ServerLevel level, Animal animal) {
        var random = animal.getRandom();
        if (random.nextFloat() < 0.15F) {
            var yaw = animal.getYRot() + (random.nextFloat() - 0.5F) * 70.0F;
            animal.setYRot(yaw);
            animal.setYHeadRot(yaw);
            animal.yBodyRot = yaw;
        }
        if (random.nextInt(20) == 0) {
            animal.getNavigation().stop();
        }
        if (animal.onGround() && random.nextInt(50) == 0) {
            animal.getJumpControl().jump();
        }
        if (animal.tickCount % 5 == 0) {
            level.sendParticles(
                PathogenZoneManager.PATHOGEN_DUST,
                animal.getX(),
                animal.getY() + animal.getBbHeight() * 0.5D,
                animal.getZ(),
                3,
                animal.getBbWidth() * 0.4D,
                animal.getBbHeight() * 0.3D,
                animal.getBbWidth() * 0.4D,
                0.01D
            );
        }
    }
}
