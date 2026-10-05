package com.azure.pathogenesis.entity.ai.hammerpede;

import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.api.goal.GoalUrgency;
import com.azure.azurecortex.goap.GoalFailureCooldowns;
import com.azure.azurecortex.goap.GoalPlanner;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.goap.PlanFeedback;
import com.azure.azurecortex.goap.PlannedGoal;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.pathogenesis.entity.HammerpedeEntity;
import com.azure.pathogenesis.entity.ai.common.CortexGlue;

public final class HammerpedeGoalPlanner implements GoalPlanner<HammerpedeEntity, HammerpedeGoal> {

    public static final String WOUNDED_COOLDOWN = "hammerpede_wounded_cd";

    private static final float WOUNDED_HEALTH = 0.35F;

    @Override
    @SuppressWarnings("unchecked")
    public PlannedGoal<HammerpedeEntity, HammerpedeGoal> chooseGoal(
        HammerpedeEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns
    ) {
        var tick = (int) agent.level().getGameTime();
        var target = blackboard.get(CommonBlackboardKeys.TARGET);

        if (
            CortexGlue.healthFraction(agent) <= WOUNDED_HEALTH && !cooldowns.isOnCooldown(WOUNDED_COOLDOWN)
                && (target != null || agent.getLastHurtByMob() != null)
        ) {
            cooldowns.set(WOUNDED_COOLDOWN, 200);
            agent.beginRetreat(120);
        }

        if (agent.isRetreating()) {
            return PlannedGoal.of(
                HammerpedeGoal.RETREAT,
                90.0F,
                tick,
                10,
                agent.retreatTicks(),
                null,
                null,
                GoalUrgency.EMERGENCY,
                true,
                "Breaking off"
            );
        }

        var failures = GoalFailureCooldowns.<HammerpedeGoal>getOrCreate(blackboard);
        failures.evictExpired(tick);
        var feedback = (PlanFeedback<HammerpedeGoal>) blackboard.get(CommonBlackboardKeys.LAST_PLAN_FEEDBACK);
        if (
            feedback != null && feedback.isFresh(tick)
                && (feedback.reason() == PlanFailureReason.FAILED_NO_PATH || feedback
                    .reason() == PlanFailureReason.FAILED_STUCK)
        ) {
            failures.recordFailure(HammerpedeGoal.STRIKE, tick);
        }

        if (target != null && target.isAlive()) {
            var score = 50.0F - failures.getPenalty(HammerpedeGoal.STRIKE, tick);
            if (score > 15.0F) {
                return PlannedGoal.of(
                    HammerpedeGoal.STRIKE,
                    score,
                    tick,
                    10,
                    160,
                    target,
                    null,
                    GoalUrgency.HIGH,
                    true,
                    "Movement in reach"
                );
            }
        }

        var heard = agent.heardPos();
        if (heard != null && tick - agent.heardTick() <= 200) {
            return PlannedGoal.of(
                HammerpedeGoal.INVESTIGATE_SOUND,
                25.0F,
                tick,
                20,
                200,
                null,
                heard,
                GoalUrgency.NORMAL,
                true,
                "Felt movement"
            );
        }

        if (!agent.isHidden()) {
            return PlannedGoal.of(
                HammerpedeGoal.HIDE,
                30.0F,
                tick,
                20,
                200,
                null,
                null,
                GoalUrgency.NORMAL,
                true,
                "Exposed"
            );
        }

        if (agent.getRandom().nextInt(6) == 0) {
            return PlannedGoal.of(
                HammerpedeGoal.WANDER,
                10.0F,
                tick,
                40,
                240,
                null,
                null,
                GoalUrgency.LOW,
                true,
                "Shifting hiding place"
            );
        }

        return PlannedGoal.of(
            HammerpedeGoal.LURK,
            12.0F,
            tick,
            40,
            300,
            null,
            null,
            GoalUrgency.LOW,
            true,
            "Lying in wait"
        );
    }
}
