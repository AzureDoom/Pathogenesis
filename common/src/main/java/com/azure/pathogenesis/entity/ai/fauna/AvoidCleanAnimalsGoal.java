package com.azure.pathogenesis.entity.ai.fauna;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class AvoidCleanAnimalsGoal extends Goal {

    private final Animal animal;

    @Nullable
    private Animal avoided;

    public AvoidCleanAnimalsGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (animal.isLeashed() || animal.getRandom().nextInt(reducedTickDelay(10)) != 0) {
            return false;
        }
        if (!ContaminatedFauna.severity(animal).atLeast(FaunaSeverity.MODERATE)) {
            return false;
        }
        avoided = nearestClean();
        if (avoided == null) {
            return false;
        }
        var away = DefaultRandomPos.getPosAway(animal, 10, 4, avoided.position());
        if (away == null || avoided.distanceToSqr(away) <= avoided.distanceToSqr(animal)) {
            return false;
        }
        return animal.getNavigation().moveTo(away.x, away.y, away.z, 1.0D);
    }

    @Override
    public boolean canContinueToUse() {
        return avoided != null && avoided.isAlive() && !animal.getNavigation().isDone()
            && animal.distanceToSqr(avoided) < 12.0D * 12.0D;
    }

    @Override
    public void stop() {
        avoided = null;
    }

    @Nullable
    private Animal nearestClean() {
        Animal nearest = null;
        var best = 6.0D * 6.0D;
        for (
            var other : animal.level()
                .getEntitiesOfClass(
                    Animal.class,
                    animal.getBoundingBox().inflate(6.0D, 3.0D, 6.0D),
                    other -> other != animal && other.isAlive() && !(other instanceof Enemy)
                )
        ) {
            var distance = animal.distanceToSqr(other);
            if (distance < best && ContaminatedFauna.isClean(other)) {
                best = distance;
                nearest = other;
            }
        }
        return nearest;
    }
}
