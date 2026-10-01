package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class FleeThreatAction<E extends PathfinderMob, G> extends PathogenAction<E, G> {

    private final Function<Blackboard, LivingEntity> threatGetter;

    private final double speed;

    private final double safeDistance;

    private final int maxDuration;

    @Nullable
    private final String cooldownKey;

    private final int cooldown;

    private int ticks;

    public FleeThreatAction(
        String name,
        int priority,
        Function<Blackboard, LivingEntity> threatGetter,
        double speed,
        double safeDistance,
        int maxDuration,
        @Nullable String cooldownKey,
        int cooldown
    ) {
        super(name, priority);
        this.threatGetter = threatGetter;
        this.speed = speed;
        this.safeDistance = safeDistance;
        this.maxDuration = maxDuration;
        this.cooldownKey = cooldownKey;
        this.cooldown = cooldown;
    }

    @Override
    public void start(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
    }

    @Override
    public ActionOutcome<G> tick(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var threat = threatGetter.apply(blackboard);
        if (threat == null || !threat.isAlive() || agent.distanceToSqr(threat) > safeDistance * safeDistance) {
            return CortexGlue.done();
        }
        if (++ticks > maxDuration) {
            return CortexGlue.done();
        }
        if (agent.getNavigation().isDone() || ticks % 20 == 0) {
            Vec3 away = DefaultRandomPos.getPosAway(agent, (int) safeDistance, 7, threat.position());
            if (away == null) {
                return CortexGlue.failed(PlanFailureReason.FAILED_NO_PATH, agent.blockPosition());
            }
            agent.getNavigation().moveTo(away.x, away.y, away.z, speed);
        }
        return CortexGlue.running();
    }

    @Override
    public void stop(E agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        super.stop(agent, blackboard, cooldowns, reason);
        if (cooldownKey != null) {
            cooldowns.set(cooldownKey, cooldown);
        }
    }
}
