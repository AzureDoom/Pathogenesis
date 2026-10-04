package com.azure.pathogenesis.entity.ai.bloodburster;

import com.azure.azurecortex.action.movement.IdleAction;
import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.behavior.composite.PrioritySelector;
import com.azure.azurecortex.behavior.decorator.Condition;
import com.azure.azurecortex.behavior.leaf.ActionNode;
import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.entity.BloodbursterEntity;
import com.azure.pathogenesis.entity.ai.common.DarkBiasedWanderAction;
import com.azure.pathogenesis.entity.ai.common.FleeThreatAction;
import com.azure.pathogenesis.entity.ai.common.InvestigateScentAction;
import com.azure.pathogenesis.entity.ai.common.PathogenBlackboardKeys;
import com.azure.pathogenesis.entity.ai.common.PathogenHuntNode;
import com.azure.pathogenesis.entity.ai.common.PathogenPriorities;
import com.azure.pathogenesis.entity.ai.common.RushTargetAction;
import com.azure.pathogenesis.entity.ai.common.WindupMeleeAction;
import org.jetbrains.annotations.NotNull;

public final class BloodbursterTree {

    private BloodbursterTree() {}

    public static BehaviorNode<BloodbursterEntity, BloodbursterGoal> create() {
        var idle = new IdleAction<BloodbursterEntity, BloodbursterGoal>();
        var wander = new DarkBiasedWanderAction<BloodbursterEntity, BloodbursterGoal>(
            "bloodburster_wander",
            5,
            0.9D,
            8,
            120
        );
        var sniff = new InvestigateScentAction<BloodbursterEntity, BloodbursterGoal>(
            "bloodburster_investigate_scent",
            PathogenPriorities.INVESTIGATE_SCENT,
            1.0D
        );
        var cover = new SeekCoverAction(PathogenPriorities.SEEK_COVER);
        var flee = new FleeThreatAction<BloodbursterEntity, BloodbursterGoal>(
            "bloodburster_flee",
            PathogenPriorities.FLEE,
            bb -> bb.get(PathogenBlackboardKeys.THREAT),
            1.5D,
            14.0D,
            160,
            null,
            0
        );

        var feed = getFeed();

        return PrioritySelector.of(
            new ActionNode<>(idle, PathogenPriorities.IDLE),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == BloodbursterGoal.WANDER,
                new ActionNode<>(wander, PathogenPriorities.WANDER)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == BloodbursterGoal.INVESTIGATE_SCENT,
                new ActionNode<>(sniff, PathogenPriorities.INVESTIGATE_SCENT)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == BloodbursterGoal.SEEK_COVER,
                new ActionNode<>(cover, PathogenPriorities.SEEK_COVER)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == BloodbursterGoal.FLEE,
                new ActionNode<>(flee, PathogenPriorities.FLEE)
            ),
            feed
        );
    }

    private static @NotNull PathogenHuntNode<BloodbursterEntity, BloodbursterGoal> getFeed() {
        var chase = new RushTargetAction<BloodbursterEntity, BloodbursterGoal>("bloodburster_chase", 15, 1.35D, 8, 60);
        var bite = new WindupMeleeAction<BloodbursterEntity, BloodbursterGoal>(
            "bloodburster_bite",
            PathogenPriorities.MELEE,
            () -> Pathogenesis.getConfig().entityConfigs.bloodbursterConfigs.bloodbursterAttackWindup,
            0.6D,
            "bloodburster_bite_cd",
            () -> Pathogenesis.getConfig().entityConfigs.bloodbursterConfigs.bloodbursterAttackCooldown,
            (agent, target) -> agent.animations().playOnce("attack", 10)
        );
        return new PathogenHuntNode<>(
            BloodbursterGoal.FEED,
            (agent, target, bb, cd) -> bite.canStart(agent, target) && !cd.isOnCooldown(bite.cooldownKey())
                ? bite
                : chase
        );
    }
}
