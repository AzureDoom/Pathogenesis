package com.azure.pathogenesis.entity.ai.fauna;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.exposure.ExposureType;
import com.azure.pathogenesis.exposure.PathogenExposureHelper;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class ContaminationFrenzyGoal extends Goal {

    private final Animal animal;

    @Nullable
    private LivingEntity target;

    private int attackCooldown;

    private int repath;

    private int chaseTicks;

    public ContaminationFrenzyGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private static double range() {
        return Pathogenesis.getConfig().faunaConfigs.frenzyRange;
    }

    @Override
    public boolean canUse() {
        if (animal.getRandom().nextInt(reducedTickDelay(10)) != 0) {
            return false;
        }
        if (!ContaminatedFauna.severity(animal).atLeast(FaunaSeverity.HIGH)) {
            return false;
        }
        target = findTarget();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        var range = range() * 1.5D;
        return target != null && isValidTarget(target) && chaseTicks < 400
            && animal.distanceToSqr(target) < range * range
            && ContaminatedFauna.severity(animal).atLeast(FaunaSeverity.HIGH);
    }

    @Override
    public void start() {
        chaseTicks = 0;
        repath = 0;
        animal.setAggressive(true);
    }

    @Override
    public void stop() {
        target = null;
        animal.getNavigation().stop();
        animal.setAggressive(false);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        var current = target;
        if (current == null) {
            return;
        }
        chaseTicks++;
        animal.getLookControl().setLookAt(current, 30.0F, 30.0F);
        if (--repath <= 0) {
            repath = 8 + animal.getRandom().nextInt(6);
            animal.getNavigation().moveTo(current, 1.4D);
        }
        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }
        var reach = animal.getBbWidth() * 2.0F;
        var reachSqr = reach * reach + current.getBbWidth();
        if (animal.distanceToSqr(current) <= reachSqr && animal.hasLineOfSight(current)) {
            bite(current);
        }
    }

    private void bite(LivingEntity victim) {
        attackCooldown = 25;
        animal.swing(InteractionHand.MAIN_HAND);
        var damage = Mth.clamp(1.5F + animal.getBbWidth() * animal.getBbHeight(), 2.0F, 6.0F);
        if (victim.hurt(animal.damageSources().mobAttack(animal), damage)) {
            victim.knockback(0.4D, animal.getX() - victim.getX(), animal.getZ() - victim.getZ());
            PathogenExposureHelper.expose(victim, 3, ExposureType.CONTACT);
        }
        animal.playAmbientSound();
    }

    @Nullable
    private LivingEntity findTarget() {
        LivingEntity nearest = null;
        var range = range();
        var best = range * range;
        for (
            var entity : animal.level()
                .getEntitiesOfClass(LivingEntity.class, animal.getBoundingBox().inflate(range, 4.0D, range))
        ) {
            var distance = animal.distanceToSqr(entity);
            if (distance < best && isValidTarget(entity) && animal.hasLineOfSight(entity)) {
                best = distance;
                nearest = entity;
            }
        }
        return nearest;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == animal || !entity.isAlive() || entity.getType().is(PathogenTags.Entities.PATHOGEN_IMMUNE)) {
            return false;
        }
        if (animal instanceof TamableAnimal tamable && tamable.isOwnedBy(entity)) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return entity instanceof Animal other && !(other instanceof Enemy) && ContaminatedFauna.isClean(other);
    }
}
