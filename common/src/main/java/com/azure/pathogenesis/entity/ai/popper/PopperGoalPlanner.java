package com.azure.pathogenesis.entity.ai.popper;

import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.api.goal.GoalUrgency;
import com.azure.azurecortex.goap.GoalPlanner;
import com.azure.azurecortex.goap.PlannedGoal;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.pathogenesis.entity.PopperEntity;

public final class PopperGoalPlanner implements GoalPlanner<PopperEntity, PopperGoal> {

    @Override
    public PlannedGoal<PopperEntity, PopperGoal> chooseGoal(
        PopperEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns
    ) {
        var tick = (int) agent.level().getGameTime();
        var target = blackboard.get(CommonBlackboardKeys.TARGET);
        if (agent.isFusing() || target != null && target.isAlive()) {
            return PlannedGoal.of(
                PopperGoal.APPROACH,
                50.0F,
                tick,
                20,
                400,
                target,
                null,
                GoalUrgency.HIGH,
                !agent.isFusing(),
                "Prey nearby"
            );
        }
        return PlannedGoal.of(
            PopperGoal.DRIFT,
            10.0F,
            tick,
            40,
            300,
            null,
            null,
            GoalUrgency.LOW,
            true,
            "Drifting"
        );
    }
}
