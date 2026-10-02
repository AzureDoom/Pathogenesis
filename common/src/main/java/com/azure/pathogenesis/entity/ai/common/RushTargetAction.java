package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class RushTargetAction<E extends PathfinderMob, G> extends PathogenAction<E, G> {

    private final double speed;

    private final int repathInterval;

    private final int stuckTicks;

    private int ticks;

    private int stillTicks;

    private Vec3 lastPos = Vec3.ZERO;

    public RushTargetAction(String name, int priority, double speed, int repathInterval, int stuckTicks) {
        super(name, priority);
        this.speed = speed;
        this.repathInterval = repathInterval;
        this.stuckTicks = stuckTicks;
    }

    @Override
    public void start(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        stillTicks = 0;
        lastPos = agent.position();
    }

    @Override
    public ActionOutcome<G> tick(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = blackboard.get(CommonBlackboardKeys.TARGET);
        if (target == null || !target.isAlive()) {
            return CortexGlue.failed(PlanFailureReason.FAILED_TARGET_LOST, agent.blockPosition());
        }
        agent.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (ticks++ % repathInterval == 0 || agent.getNavigation().isDone()) {
            if (!agent.getNavigation().moveTo(target, speed)) {
                return CortexGlue.failed(PlanFailureReason.FAILED_NO_PATH, target.blockPosition());
            }
        }

        var contact = agent.getBbWidth() + target.getBbWidth() + 1.5D;
        var position = agent.position();
        var moved = position.distanceToSqr(lastPos);
        lastPos = position;
        if (agent.distanceToSqr(target) <= contact * contact || moved > 0.0025D) {
            stillTicks = 0;
        } else if (++stillTicks > stuckTicks) {
            return CortexGlue.failed(PlanFailureReason.FAILED_STUCK, agent.blockPosition());
        }
        return CortexGlue.running();
    }
}
