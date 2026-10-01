package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.action.Action;
import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.behavior.BehaviorResult;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

public final class PathogenHuntNode<E extends Mob, G> implements BehaviorNode<E, G> {

    @FunctionalInterface
    public interface Chooser<E, G> {

        @Nullable
        Action<E, G> choose(E agent, LivingEntity target, Blackboard blackboard, CooldownTracker cooldowns);
    }

    private final G huntGoalType;

    private final Chooser<E, G> chooser;

    public PathogenHuntNode(G huntGoalType, Chooser<E, G> chooser) {
        this.huntGoalType = huntGoalType;
        this.chooser = chooser;
    }

    @Override
    public BehaviorResult<E, G> tick(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (!huntGoalType.equals(blackboard.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE))) {
            return BehaviorResult.none();
        }
        var target = blackboard.get(CommonBlackboardKeys.TARGET);
        if (target == null || !target.isAlive()) {
            return BehaviorResult.none();
        }
        var action = chooser.choose(agent, target, blackboard, cooldowns);
        return action == null ? BehaviorResult.none() : BehaviorResult.run(action, action.priority());
    }
}
