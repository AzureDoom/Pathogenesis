package com.azure.pathogenesis.entity.ai.neophyte;

import com.azure.azurecortex.action.movement.IdleAction;
import com.azure.azurecortex.action.utility.InvestigateLastSeenTargetAction;
import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.behavior.composite.PrioritySelector;
import com.azure.azurecortex.behavior.decorator.Condition;
import com.azure.azurecortex.behavior.leaf.ActionNode;
import com.azure.azurecortex.navigation.astar.AStarPathfinder;
import com.azure.pathogenesis.entity.NeophyteEntity;
import com.azure.pathogenesis.entity.ai.common.DarkBiasedWanderAction;
import com.azure.pathogenesis.entity.ai.common.MoveAwayAction;
import com.azure.pathogenesis.entity.ai.common.PathogenHuntNode;
import com.azure.pathogenesis.entity.ai.common.RushTargetAction;
import com.azure.pathogenesis.entity.ai.common.WindupMeleeAction;
import com.azure.pathogenesis.entity.ai.neomorph.InvestigateSoundAction;
import org.jetbrains.annotations.NotNull;

public final class NeophyteTree {

    public static final String MELEE_COOLDOWN = "neophyte_melee_cd";

    private NeophyteTree() {}

    public static BehaviorNode<NeophyteEntity, NeophyteGoal> create() {
        var idle = new IdleAction<NeophyteEntity, NeophyteGoal>();
        var roam = new DarkBiasedWanderAction<NeophyteEntity, NeophyteGoal>("neophyte_roam", 5, 0.9D, 16, 160);
        var listen = new InvestigateSoundAction<NeophyteEntity, NeophyteGoal>("neophyte_investigate_sound", 7);
        var investigate = new InvestigateLastSeenTargetAction<NeophyteEntity, NeophyteGoal>(
            AStarPathfinder.INSTANCE,
            1.05D,
            2,
            60,
            120,
            60,
            0.02D,
            2.0D,
            8.0D
        );
        var retreat = new MoveAwayAction<NeophyteEntity, NeophyteGoal>(
            "neophyte_retreat",
            NeophyteEntity::retreatFrom,
            1.35D,
            20.0D,
            15,
            160,
            40
        );

        var hunt = getHunt();

        return PrioritySelector.of(
            new ActionNode<>(idle, 0),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == NeophyteGoal.ROAM,
                new ActionNode<>(roam, 5)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == NeophyteGoal.INVESTIGATE_SOUND,
                new ActionNode<>(listen, 7)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == NeophyteGoal.INVESTIGATE,
                new ActionNode<>(investigate, 8)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == NeophyteGoal.RETREAT,
                new ActionNode<>(retreat, 40)
            ),
            hunt
        );
    }

    private static @NotNull PathogenHuntNode<NeophyteEntity, NeophyteGoal> getHunt() {
        var melee = new WindupMeleeAction<NeophyteEntity, NeophyteGoal>(
            "neophyte_slash",
            22,
            8,
            1.2D,
            MELEE_COOLDOWN,
            24,
            NeophyteEntity::onSlash
        );
        var rush = new RushTargetAction<NeophyteEntity, NeophyteGoal>("neophyte_rush", 15, 1.3D, 8, 80);

        return new PathogenHuntNode<>(
            NeophyteGoal.HUNT,
            (agent, target, blackboard, cooldowns) -> {
                if (melee.inReach(agent, target) && !cooldowns.isOnCooldown(MELEE_COOLDOWN)) {
                    return melee;
                }
                return rush;
            }
        );
    }
}
