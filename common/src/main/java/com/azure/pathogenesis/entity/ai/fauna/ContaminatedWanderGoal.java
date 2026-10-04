package com.azure.pathogenesis.entity.ai.fauna;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;

import java.util.EnumSet;

public class ContaminatedWanderGoal extends Goal {

    private final Animal animal;

    private int burstTicks;

    private int retarget;

    private int cooldown;

    public ContaminatedWanderGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        var severity = ContaminatedFauna.severity(animal);
        if (!severity.atLeast(FaunaSeverity.MODERATE) || animal.isPassenger() || animal.isLeashed()) {
            return false;
        }
        return animal.getRandom().nextInt(severity == FaunaSeverity.MODERATE ? 40 : 20) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return burstTicks > 0 && ContaminatedFauna.severity(animal).atLeast(FaunaSeverity.MODERATE);
    }

    @Override
    public void start() {
        burstTicks = 40 + animal.getRandom().nextInt(61);
        repath();
    }

    @Override
    public void tick() {
        burstTicks--;
        if (--retarget <= 0 || animal.getNavigation().isDone()) {
            repath();
        }
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();
        cooldown = 60 + animal.getRandom().nextInt(121);
    }

    private void repath() {
        var random = animal.getRandom();
        retarget = 10 + random.nextInt(16);
        var target = DefaultRandomPos.getPos(animal, 5, 2);
        if (target != null) {
            animal.getNavigation().moveTo(target.x, target.y, target.z, 0.9D + random.nextDouble() * 0.4D);
        }
    }
}
