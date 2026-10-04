package com.azure.pathogenesis.entity.ai.fauna;

import com.azure.pathogenesis.contamination.PathogenZoneManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class ContaminationPanicGoal extends Goal {

    private final Animal animal;

    private final boolean drawnIn;

    private int runTicks;

    private int rest;

    @Nullable
    private Vec3 zoneCenter;

    public ContaminationPanicGoal(Animal animal, boolean drawnIn) {
        this.animal = animal;
        this.drawnIn = drawnIn;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (rest > 0) {
            rest--;
            return false;
        }
        return !animal.isLeashed() && ContaminatedFauna.severity(animal).atLeast(FaunaSeverity.HIGH);
    }

    @Override
    public boolean canContinueToUse() {
        return runTicks > 0 && ContaminatedFauna.severity(animal).atLeast(FaunaSeverity.HIGH);
    }

    @Override
    public void start() {
        runTicks = 60 + animal.getRandom().nextInt(81);
        zoneCenter = null;
        if (animal.level() instanceof ServerLevel level) {
            var zone = PathogenZoneManager.findZone(level, animal.blockPosition());
            if (zone != null) {
                zoneCenter = Vec3.atBottomCenterOf(zone.origin());
            }
        }
        repath();
    }

    @Override
    public void tick() {
        runTicks--;
        if (animal.getNavigation().isDone()) {
            repath();
        }
    }

    @Override
    public void stop() {
        animal.getNavigation().stop();
        rest = 30 + animal.getRandom().nextInt(61);
    }

    private void repath() {
        Vec3 target;
        if (zoneCenter == null) {
            target = DefaultRandomPos.getPos(animal, 8, 4);
        } else if (drawnIn) {
            target = animal.distanceToSqr(zoneCenter) < 9.0D
                ? DefaultRandomPos.getPos(animal, 4, 2)
                : DefaultRandomPos.getPosTowards(animal, 10, 5, zoneCenter, Math.PI / 2.0D);
        } else {
            target = DefaultRandomPos.getPosAway(animal, 16, 7, zoneCenter);
        }
        if (target != null) {
            animal.getNavigation().moveTo(target.x, target.y, target.z, drawnIn ? 1.25D : 1.6D);
        }
    }
}
