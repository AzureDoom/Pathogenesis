package com.azure.pathogenesis.entity.ai.hammerpede;

import com.azure.azurecortex.action.movement.IdleAction;
import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.behavior.composite.PrioritySelector;
import com.azure.azurecortex.behavior.decorator.Condition;
import com.azure.azurecortex.behavior.leaf.ActionNode;
import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.config.PathogenesisConfig;
import com.azure.pathogenesis.entity.HammerpedeEntity;
import com.azure.pathogenesis.entity.ai.common.MoveAwayAction;
import com.azure.pathogenesis.entity.ai.common.PathogenHuntNode;
import com.azure.pathogenesis.entity.ai.common.PathogenPriorities;
import com.azure.pathogenesis.entity.ai.common.RushTargetAction;
import com.azure.pathogenesis.entity.ai.common.WindupMeleeAction;
import com.azure.pathogenesis.entity.ai.neomorph.InvestigateSoundAction;
import org.jetbrains.annotations.NotNull;

public final class HammerpedeTree {

    public static final String BITE_COOLDOWN = "hammerpede_bite_cd";

    private HammerpedeTree() {}

    private static PathogenesisConfig.EntityConfigs.HammerpedeConfigs config() {
        return Pathogenesis.getConfig().entityConfigs.hammerpedeConfigs;
    }

    public static BehaviorNode<HammerpedeEntity, HammerpedeGoal> create() {
        var idle = new IdleAction<HammerpedeEntity, HammerpedeGoal>();
        var lurk = new IdleAction<HammerpedeEntity, HammerpedeGoal>();
        var hide = new HideAction("hammerpede_hide", PathogenPriorities.SEEK_COVER, 1.2D, 12);
        var wander = new ShelterRoamAction("hammerpede_wander", PathogenPriorities.WANDER, 0.85D, 10, 240);
        var listen = new InvestigateSoundAction<HammerpedeEntity, HammerpedeGoal>(
            "hammerpede_investigate_sound",
            PathogenPriorities.INVESTIGATE_SOUND
        );
        var retreat = new MoveAwayAction<HammerpedeEntity, HammerpedeGoal>(
            "hammerpede_retreat",
            HammerpedeEntity::retreatFrom,
            1.45D,
            12.0D,
            10,
            160,
            PathogenPriorities.FLEE
        );

        return PrioritySelector.of(
            new ActionNode<>(idle, PathogenPriorities.IDLE),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == HammerpedeGoal.LURK,
                new ActionNode<>(lurk, PathogenPriorities.IDLE + 1)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == HammerpedeGoal.WANDER,
                new ActionNode<>(wander, PathogenPriorities.WANDER)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == HammerpedeGoal.INVESTIGATE_SOUND,
                new ActionNode<>(listen, PathogenPriorities.INVESTIGATE_SOUND)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == HammerpedeGoal.HIDE,
                new ActionNode<>(hide, PathogenPriorities.SEEK_COVER)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == HammerpedeGoal.RETREAT,
                new ActionNode<>(retreat, PathogenPriorities.FLEE)
            ),
            getStrike()
        );
    }

    private static @NotNull PathogenHuntNode<HammerpedeEntity, HammerpedeGoal> getStrike() {
        var creep = new RushTargetAction<HammerpedeEntity, HammerpedeGoal>(
            "hammerpede_creep",
            PathogenPriorities.CHASE,
            1.1D,
            6,
            60
        );
        var lunge = new LungeAction(PathogenPriorities.LEAP);
        var bite = new WindupMeleeAction<HammerpedeEntity, HammerpedeGoal>(
            "hammerpede_bite",
            PathogenPriorities.MELEE,
            () -> config().hammerpedeBiteWindup,
            0.5D,
            BITE_COOLDOWN,
            () -> config().hammerpedeBiteCooldown,
            HammerpedeEntity::onBite
        );
        return new PathogenHuntNode<>(
            HammerpedeGoal.STRIKE,
            (agent, target, bb, cd) -> {
                if (bite.canStart(agent, target) && !cd.isOnCooldown(BITE_COOLDOWN)) {
                    return bite;
                }
                if (!cd.isOnCooldown(LungeAction.COOLDOWN) && lunge.canStart(agent, target)) {
                    return lunge;
                }
                return creep;
            }
        );
    }
}
