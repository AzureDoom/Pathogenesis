package com.azure.pathogenesis.entity.ai.neophyte;

import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.api.goal.GoalUrgency;
import com.azure.azurecortex.goap.GoalFailureCooldowns;
import com.azure.azurecortex.goap.GoalPlanner;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.goap.PlanFeedback;
import com.azure.azurecortex.goap.PlannedGoal;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.pathogenesis.entity.NeophyteEntity;
import com.azure.pathogenesis.entity.ai.common.CortexGlue;
import com.azure.pathogenesis.entity.ai.common.PathogenBlackboardKeys;

public final class NeophyteGoalPlanner implements GoalPlanner<NeophyteEntity, NeophyteGoal> {

    @Override
    @SuppressWarnings("unchecked")
    public PlannedGoal<NeophyteEntity, NeophyteGoal> chooseGoal(
        NeophyteEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns
    ) {
        var tick = (int) agent.level().getGameTime();
        var target = blackboard.get(CommonBlackboardKeys.TARGET);

        if (
            CortexGlue.healthFraction(agent) <= PathogenBlackboardKeys.RETREAT_HEALTH && !cooldowns.isOnCooldown(
                PathogenBlackboardKeys.RETREAT_COOLDOWN_NEOPHYTE
            )
                && (target != null || agent.getLastHurtByMob() != null)
        ) {
            cooldowns.set(PathogenBlackboardKeys.RETREAT_COOLDOWN_NEOPHYTE, 300);
            return PlannedGoal.of(
                NeophyteGoal.RETREAT,
                90.0F,
                tick,
                80,
                160,
                null,
                null,
                GoalUrgency.EMERGENCY,
                true,
                "Wounded"
            );
        }

        var failures = GoalFailureCooldowns.<NeophyteGoal>getOrCreate(blackboard);
        failures.evictExpired(tick);
        var feedback = (PlanFeedback<NeophyteGoal>) blackboard.get(CommonBlackboardKeys.LAST_PLAN_FEEDBACK);
        if (
            feedback != null && feedback.isFresh(tick)
                && (feedback.reason() == PlanFailureReason.FAILED_STUCK || feedback
                    .reason() == PlanFailureReason.FAILED_NO_PATH)
        ) {
            failures.recordFailure(NeophyteGoal.HUNT, tick, 80);
        }

        if (target != null && target.isAlive()) {
            float score = 50.0F - failures.getPenalty(NeophyteGoal.HUNT, tick);
            if (score > 20.0F) {
                return PlannedGoal.of(
                    NeophyteGoal.HUNT,
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
        if (lastSeen != null && lastSeenTick != null && tick - lastSeenTick <= 120) {
            return PlannedGoal.of(
                NeophyteGoal.INVESTIGATE,
                30.0F,
                tick,
                20,
                120,
                null,
                lastSeen,
                GoalUrgency.NORMAL,
                true,
                "Lost sight of prey"
            );
        }

        var heard = agent.heardPos();
        if (heard != null && tick - agent.heardTick() <= 160) {
            return PlannedGoal.of(
                NeophyteGoal.INVESTIGATE_SOUND,
                25.0F,
                tick,
                20,
                160,
                null,
                heard,
                GoalUrgency.NORMAL,
                true,
                "Heard something"
            );
        }

        var scent = agent.scent();
        var scentPos = scent.target();
        if (scentPos != null) {
            if (tick - scent.acquiredTick() > 400) {
                scent.clear();
            } else {
                return PlannedGoal.of(
                    NeophyteGoal.INVESTIGATE_SCENT,
                    20.0F,
                    tick,
                    20,
                    400,
                    null,
                    scentPos,
                    GoalUrgency.NORMAL,
                    true,
                    "Smells blood"
                );
            }
        }

        return PlannedGoal.of(NeophyteGoal.ROAM, 10.0F, tick, 40, 300, null, null, GoalUrgency.LOW, true, "Roaming");
    }
}
