package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.action.Action;
import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;

import java.util.function.Function;

public class MoveAwayAction<E extends PathfinderMob, G> implements Action<E, G> {

    private final String name;

    private final Function<E, Entity> threat;

    private final double speed;

    private final double safeDistance;

    private final int repathInterval;

    private final int maxDuration;

    private final int priority;

    private int ticks;

    public MoveAwayAction(
        String name,
        Function<E, Entity> threat,
        double speed,
        double safeDistance,
        int repathInterval,
        int maxDuration,
        int priority
    ) {
        this.name = name;
        this.threat = threat;
        this.speed = speed;
        this.safeDistance = safeDistance;
        this.repathInterval = repathInterval;
        this.maxDuration = maxDuration;
        this.priority = priority;
    }

    @Override
    public void start(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
    }

    @Override
    public ActionOutcome<G> tick(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var from = threat.apply(agent);
        if (
            from == null || !from.isAlive() || agent.distanceToSqr(from) > safeDistance * safeDistance
                || ++ticks > maxDuration
        ) {
            return ActionOutcome.success();
        }
        if (ticks % repathInterval == 1 || agent.getNavigation().isDone()) {
            var away = DefaultRandomPos.getPosAway(agent, (int) safeDistance, 7, from.position());
            if (away == null) {
                return ActionOutcome.blocked(PlanFailureReason.FAILED_NO_PATH, agent.blockPosition());
            }
            agent.getNavigation().moveTo(away.x, away.y, away.z, speed);
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(E agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
    }

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public String debugName() {
        return name;
    }
}
