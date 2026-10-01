package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.api.goal.Goal;
import com.azure.azurecortex.goap.EmergencyDetector;
import com.azure.azurecortex.goap.GoalExecutor;
import com.azure.azurecortex.goap.GoalPlanner;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CortexRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.function.Predicate;

public final class CortexGlue {

    private CortexGlue() {}

    public static <E extends Mob, G extends Goal> void tickPlanner(
        E agent,
        CortexRuntime<E, G> runtime,
        GoalPlanner<E, G> planner,
        List<EmergencyDetector.EmergencyProbe<E>> probes,
        Predicate<Object> isPassive,
        boolean reactiveTrigger
    ) {
        var blackboard = runtime.getBlackboard();
        var cooldowns = runtime.getCooldowns();
        var currentTick = (int) agent.level().getGameTime();

        var active = blackboard.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE);
        var reactiveReplan = (active == null || isPassive.test(active)) && reactiveTrigger;
        var urgency = EmergencyDetector.detectPreplanUrgency(agent, probes);

        if (!reactiveReplan && urgency == null && cooldowns.isOnCooldown(CommonBlackboardKeys.GOAL_REPLAN)) {
            return;
        }
        if (!reactiveReplan && !GoalExecutor.shouldReplan(blackboard, currentTick, urgency, agent)) {
            return;
        }
        cooldowns.set(CommonBlackboardKeys.GOAL_REPLAN, 20);
        GoalExecutor.apply(agent, blackboard, planner.chooseGoal(agent, blackboard, cooldowns));
    }

    public static float healthFraction(LivingEntity entity) {
        return entity.getMaxHealth() > 0.0F ? entity.getHealth() / entity.getMaxHealth() : 1.0F;
    }

    public static <G> ActionOutcome<G> running() {
        return ActionOutcome.running();
    }

    public static <G> ActionOutcome<G> done() {
        return ActionOutcome.success();
    }

    public static <G> ActionOutcome<G> failed(PlanFailureReason reason, BlockPos pos) {
        return ActionOutcome.failed(reason, pos);
    }
}
