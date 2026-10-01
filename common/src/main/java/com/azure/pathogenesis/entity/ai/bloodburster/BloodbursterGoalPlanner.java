package com.azure.pathogenesis.entity.ai.bloodburster;

import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.api.goal.GoalUrgency;
import com.azure.azurecortex.goap.GoalFailureCooldowns;
import com.azure.azurecortex.goap.GoalPlanner;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.goap.PlanFeedback;
import com.azure.azurecortex.goap.PlannedGoal;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.pathogenesis.entity.BloodbursterEntity;
import com.azure.pathogenesis.entity.ai.common.PathogenBlackboardKeys;

public final class BloodbursterGoalPlanner implements GoalPlanner<BloodbursterEntity, BloodbursterGoal> {

    private static final int MIN_COMMIT = 20;

    private static final int MAX_COMMIT = 200;

    @Override
    @SuppressWarnings("unchecked")
    public PlannedGoal<BloodbursterEntity, BloodbursterGoal> chooseGoal(
        BloodbursterEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns
    ) {
        var tick = (int) agent.level().getGameTime();

        var threat = blackboard.get(PathogenBlackboardKeys.THREAT);
        if (threat != null && threat.isAlive()) {
            return PlannedGoal.of(
                BloodbursterGoal.FLEE,
                90.0F,
                tick,
                10,
                120,
                threat,
                null,
                GoalUrgency.HIGH,
                true,
                "Threat nearby"
            );
        }

        var failures = GoalFailureCooldowns.<BloodbursterGoal>getOrCreate(blackboard);
        failures.evictExpired(tick);
        var feedback = (PlanFeedback<BloodbursterGoal>) blackboard.get(CommonBlackboardKeys.LAST_PLAN_FEEDBACK);
        if (
            feedback != null && feedback.isFresh(tick)
                && (feedback.reason() == PlanFailureReason.FAILED_NO_PATH || feedback
                    .reason() == PlanFailureReason.FAILED_STUCK)
        ) {
            failures.recordFailure(BloodbursterGoal.FEED, tick);
        }

        var prey = blackboard.get(CommonBlackboardKeys.TARGET);
        if (agent.isHungry() && prey != null && prey.isAlive()) {
            var score = 50.0F - failures.getPenalty(BloodbursterGoal.FEED, tick);
            if (score > 15.0F) {
                return PlannedGoal.of(
                    BloodbursterGoal.FEED,
                    score,
                    tick,
                    MIN_COMMIT,
                    MAX_COMMIT,
                    prey,
                    null,
                    GoalUrgency.NORMAL,
                    true,
                    "Hungry, prey in range"
                );
            }
        }

        if (agent.isExposed()) {
            return PlannedGoal.of(
                BloodbursterGoal.SEEK_COVER,
                30.0F,
                tick,
                MIN_COMMIT,
                MAX_COMMIT,
                null,
                null,
                GoalUrgency.NORMAL,
                true,
                "Exposed in the open"
            );
        }

        return PlannedGoal.of(
            BloodbursterGoal.WANDER,
            10.0F,
            tick,
            MIN_COMMIT,
            MAX_COMMIT,
            null,
            null,
            GoalUrgency.LOW,
            true,
            "Nothing better to do"
        );
    }
}
