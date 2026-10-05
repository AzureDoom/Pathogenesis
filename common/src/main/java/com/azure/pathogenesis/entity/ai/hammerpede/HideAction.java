package com.azure.pathogenesis.entity.ai.hammerpede;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.pathogenesis.entity.HammerpedeEntity;
import com.azure.pathogenesis.entity.ai.common.CortexGlue;
import com.azure.pathogenesis.entity.ai.common.PathogenAction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class HideAction extends PathogenAction<HammerpedeEntity, HammerpedeGoal> {

    private final double speed;

    private final int range;

    @Nullable
    private Vec3 spot;

    private int ticks;

    public HideAction(String name, int priority, double speed, int range) {
        super(name, priority);
        this.speed = speed;
        this.range = range;
    }

    @Nullable
    public static Vec3 findSpot(HammerpedeEntity agent, int range, int attempts) {
        Vec3 best = null;
        var bestScore = Integer.MIN_VALUE;
        for (var i = 0; i < attempts; i++) {
            var candidate = DefaultRandomPos.getPos(agent, range, 4);
            if (candidate == null) {
                continue;
            }
            var score = HammerpedeEntity.hidingScore(agent.level(), BlockPos.containing(candidate));
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    @Override
    public void start(HammerpedeEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        spot = findSpot(agent, range, 12);
        if (spot != null) {
            agent.getNavigation().moveTo(spot.x, spot.y, spot.z, speed);
        }
    }

    @Override
    public ActionOutcome<HammerpedeGoal> tick(
        HammerpedeEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns
    ) {
        if (agent.isHidden() && (spot == null || agent.position().distanceToSqr(spot) < 2.25D)) {
            return CortexGlue.done();
        }
        if (spot == null) {
            return CortexGlue.failed(PlanFailureReason.FAILED_NO_VALID_PLACEMENT, agent.blockPosition());
        }
        if (++ticks > 200) {
            return CortexGlue.failed(PlanFailureReason.FAILED_STUCK, agent.blockPosition());
        }
        if (agent.getNavigation().isDone()) {
            if (agent.isHidden()) {
                return CortexGlue.done();
            }
            spot = findSpot(agent, range, 12);
            if (spot == null || !agent.getNavigation().moveTo(spot.x, spot.y, spot.z, speed)) {
                return CortexGlue.failed(PlanFailureReason.FAILED_NO_PATH, agent.blockPosition());
            }
        }
        return CortexGlue.running();
    }
}
