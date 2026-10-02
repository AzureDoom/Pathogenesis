package com.azure.pathogenesis.entity.ai.neomorph;

import com.azure.azurecortex.action.movement.IdleAction;
import com.azure.azurecortex.action.utility.InvestigateLastSeenTargetAction;
import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.behavior.composite.PrioritySelector;
import com.azure.azurecortex.behavior.decorator.Condition;
import com.azure.azurecortex.behavior.leaf.ActionNode;
import com.azure.azurecortex.navigation.astar.AStarPathfinder;
import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.config.PathogenesisConfig;
import com.azure.pathogenesis.entity.NeomorphEntity;
import com.azure.pathogenesis.entity.ai.common.DarkBiasedWanderAction;
import com.azure.pathogenesis.entity.ai.common.MoveAwayAction;
import com.azure.pathogenesis.entity.ai.common.PathogenHuntNode;
import com.azure.pathogenesis.entity.ai.common.RushTargetAction;
import com.azure.pathogenesis.entity.ai.common.WindupMeleeAction;
import org.jetbrains.annotations.NotNull;

public final class NeomorphTree {

    public static final String MELEE_COOLDOWN = "neomorph_melee_cd";

    private NeomorphTree() {}

    private static PathogenesisConfig.EntityConfigs.NeomorphConfigs config() {
        return Pathogenesis.getConfig().entityConfigs.neomorphConfigs;
    }

    public static BehaviorNode<NeomorphEntity, NeomorphGoal> create() {
        var idle = new IdleAction<NeomorphEntity, NeomorphGoal>();
        var roam = new DarkBiasedWanderAction<NeomorphEntity, NeomorphGoal>("neomorph_roam", 5, 0.9D, 24, 200);
        var listen = new InvestigateSoundAction<NeomorphEntity, NeomorphGoal>("neomorph_investigate_sound", 7);
        var investigate = new InvestigateLastSeenTargetAction<NeomorphEntity, NeomorphGoal>(
            AStarPathfinder.INSTANCE,
            1.1D,
            2,
            60,
            160,
            60,
            0.02D,
            2.0D,
            10.0D
        );
        var retreat = new MoveAwayAction<NeomorphEntity, NeomorphGoal>(
            "neomorph_retreat",
            NeomorphEntity::retreatFrom,
            1.4D,
            24.0D,
            15,
            140,
            40
        );

        var hunt = getHunt();

        return PrioritySelector.of(
            new ActionNode<>(idle, 0),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == NeomorphGoal.ROAM,
                new ActionNode<>(roam, 5)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == NeomorphGoal.INVESTIGATE_SOUND,
                new ActionNode<>(listen, 7)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == NeomorphGoal.INVESTIGATE,
                new ActionNode<>(investigate, 8)
            ),
            new Condition<>(
                (a, bb, cd) -> bb.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) == NeomorphGoal.RETREAT,
                new ActionNode<>(retreat, 40)
            ),
            hunt
        );
    }

    private static @NotNull PathogenHuntNode<NeomorphEntity, NeomorphGoal> getHunt() {
        var leap = new LeapAttackAction(25);
        var melee = new WindupMeleeAction<NeomorphEntity, NeomorphGoal>(
            "neomorph_slash",
            22,
            () -> config().neomorphAttackWindup,
            1.6D,
            MELEE_COOLDOWN,
            () -> config().neomorphAttackCooldown,
            NeomorphEntity::onSlash
        );
        var stalk = new StalkTargetAction(16);
        var rush = new RushTargetAction<NeomorphEntity, NeomorphGoal>("neomorph_rush", 15, 1.45D, 6, 80);

        return new PathogenHuntNode<>(
            NeomorphGoal.HUNT,
            (agent, target, blackboard, cooldowns) -> {
                double distSqr = agent.distanceToSqr(target);
                if (melee.canStart(agent, target) && !cooldowns.isOnCooldown(MELEE_COOLDOWN)) {
                    return melee;
                }
                if (
                    distSqr >= 9.0D && distSqr <= 64.0D && agent.onGround()
                        && !cooldowns.isOnCooldown(LeapAttackAction.COOLDOWN) && agent.hasLineOfSight(target)
                ) {
                    return leap;
                }
                if (distSqr > 100.0D && agent.shouldStalk(target)) {
                    return stalk;
                }
                return rush;
            }
        );
    }
}
