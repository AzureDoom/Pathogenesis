package com.azure.pathogenesis.entity.ai.neomorph;

import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.api.goal.GoalUrgency;
import com.azure.azurecortex.goap.GoalFailureCooldowns;
import com.azure.azurecortex.goap.GoalPlanner;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.goap.PlanFeedback;
import com.azure.azurecortex.goap.PlannedGoal;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.pathogenesis.entity.NeomorphEntity;
import com.azure.pathogenesis.entity.ai.common.CortexGlue;

public final class NeomorphGoalPlanner implements GoalPlanner<NeomorphEntity, NeomorphGoal> {

    public static final float RETREAT_HEALTH = 0.30F;

    public static final String RETREAT_COOLDOWN = "neomorph_retreat_cd";

    public static final int RETREAT_COOLDOWN_TICKS = 400;

    private static final int INVESTIGATE_MAX_AGE = 160;

    private static final int SOUND_MAX_AGE = 200;

    @Override
    @SuppressWarnings("unchecked")
    public PlannedGoal<NeomorphEntity, NeomorphGoal> chooseGoal(
        NeomorphEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns
    ) {
        var tick = (int) agent.level().getGameTime();
        var target = blackboard.get(CommonBlackboardKeys.TARGET);

        if (
            CortexGlue.healthFraction(agent) <= RETREAT_HEALTH && !cooldowns.isOnCooldown(RETREAT_COOLDOWN)
                && (target != null || agent.getLastHurtByMob() != null)
        ) {
            cooldowns.set(RETREAT_COOLDOWN, RETREAT_COOLDOWN_TICKS);
            return PlannedGoal.of(
                NeomorphGoal.RETREAT,
                90.0F,
                tick,
                80,
                140,
                null,
                null,
                GoalUrgency.EMERGENCY,
                true,
                "Badly wounded"
            );
        }

        var failures = GoalFailureCooldowns.<NeomorphGoal>getOrCreate(blackboard);
        failures.evictExpired(tick);
        var feedback = (PlanFeedback<NeomorphGoal>) blackboard.get(CommonBlackboardKeys.LAST_PLAN_FEEDBACK);
        if (
            feedback != null && feedback.isFresh(tick)
                && (feedback.reason() == PlanFailureReason.FAILED_STUCK || feedback
                    .reason() == PlanFailureReason.FAILED_NO_PATH)
        ) {
            failures.recordFailure(NeomorphGoal.HUNT, tick, 80);
        }

        if (target != null && target.isAlive()) {
            float score = 50.0F - failures.getPenalty(NeomorphGoal.HUNT, tick);
            if (score > 20.0F) {
                return PlannedGoal.of(
                    NeomorphGoal.HUNT,
                    score,
                    tick,
                    20,
                    200,
                    target,
                    null,
                    GoalUrgency.NORMAL,
                    true,
                    "Prey acquired"
                );
            }
        }

        var lastSeen = blackboard.get(CommonBlackboardKeys.LAST_SEEN_POS);
        var lastSeenTick = blackboard.get(CommonBlackboardKeys.LAST_SEEN_TICK);
        if (lastSeen != null && lastSeenTick != null && tick - lastSeenTick <= INVESTIGATE_MAX_AGE) {
            return PlannedGoal.of(
                NeomorphGoal.INVESTIGATE,
                30.0F,
                tick,
                20,
                160,
                null,
                lastSeen,
                GoalUrgency.NORMAL,
                true,
                "Lost sight of prey"
            );
        }

        var heard = agent.heardPos();
        if (heard != null && tick - agent.heardTick() <= SOUND_MAX_AGE) {
            return PlannedGoal.of(
                NeomorphGoal.INVESTIGATE_SOUND,
                25.0F,
                tick,
                20,
                200,
                null,
                heard,
                GoalUrgency.NORMAL,
                true,
                "Heard something"
            );
        }

        return PlannedGoal.of(NeomorphGoal.ROAM, 10.0F, tick, 40, 400, null, null, GoalUrgency.LOW, true, "Roaming");
    }
}
