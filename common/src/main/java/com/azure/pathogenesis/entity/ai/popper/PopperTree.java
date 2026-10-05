package com.azure.pathogenesis.entity.ai.popper;

import com.azure.azurecortex.action.movement.IdleAction;
import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.behavior.composite.PrioritySelector;
import com.azure.azurecortex.behavior.decorator.Condition;
import com.azure.azurecortex.behavior.leaf.ActionNode;
import com.azure.pathogenesis.entity.PopperEntity;
import com.azure.pathogenesis.entity.ai.common.PathogenHuntNode;
import com.azure.pathogenesis.entity.ai.common.PathogenPriorities;
import com.azure.pathogenesis.entity.ai.common.RoamAction;
import com.azure.pathogenesis.entity.ai.common.RushTargetAction;

public final class PopperTree {

    private PopperTree() {}

    public static BehaviorNode<PopperEntity, PopperGoal> create() {
        var idle = new IdleAction<PopperEntity, PopperGoal>();
        var drift = new RoamAction<PopperEntity, PopperGoal>("popper_drift", PathogenPriorities.WANDER, 0.7D, 5, 300);
        var crawl = new RushTargetAction<PopperEntity, PopperGoal>(
            "popper_crawl",
            PathogenPriorities.CHASE,
            1.0D,
            10,
            100
        );
        var detonate = new DetonateAction(PathogenPriorities.LEAP);

        return PrioritySelector.of(
            new ActionNode<>(idle, PathogenPriorities.IDLE),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == PopperGoal.DRIFT,
                new ActionNode<>(drift, PathogenPriorities.WANDER)
            ),
            new Condition<>(
                (a, bb, cd) -> a.isFusing(),
                new ActionNode<>(detonate, PathogenPriorities.LEAP)
            ),
            new PathogenHuntNode<>(
                PopperGoal.APPROACH,
                (agent, target, bb, cd) -> detonate.canStart(agent, target) ? detonate : crawl
            )
        );
    }
}
