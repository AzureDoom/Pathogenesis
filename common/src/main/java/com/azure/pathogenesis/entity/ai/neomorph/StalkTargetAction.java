package com.azure.pathogenesis.entity.ai.neomorph;

import com.azure.azurecortex.api.action.Action;
import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.pathogenesis.entity.NeomorphEntity;

public class StalkTargetAction implements Action<NeomorphEntity, NeomorphGoal> {

    private static final double STALK_SPEED = 0.55D;

    private static final double CLOSE_ENOUGH_SQR = 100.0D;

    private final int priority;

    private int repath;

    public StalkTargetAction(int priority) {
        this.priority = priority;
    }

    @Override
    public void start(NeomorphEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        repath = 0;
        agent.setStalking(true);
    }

    @Override
    public ActionOutcome<NeomorphGoal> tick(NeomorphEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = blackboard.get(CommonBlackboardKeys.TARGET);
        if (target == null || !target.isAlive()) {
            return ActionOutcome.failed(PlanFailureReason.FAILED_TARGET_LOST, agent.blockPosition());
        }
        if (agent.distanceToSqr(target) <= CLOSE_ENOUGH_SQR || !agent.shouldStalk(target)) {
            return ActionOutcome.success();
        }
        agent.getLookControl().setLookAt(target, 10.0F, 10.0F);
        if (--repath <= 0) {
            repath = 20;
            if (!agent.getNavigation().moveTo(target, STALK_SPEED)) {
                return ActionOutcome.failed(PlanFailureReason.FAILED_NO_PATH, target.blockPosition());
            }
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(NeomorphEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.setStalking(false);
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
        return "neomorph_stalk";
    }
}
