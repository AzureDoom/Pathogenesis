package com.azure.pathogenesis.entity.ai.bloodburster;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.pathogenesis.entity.BloodbursterEntity;
import com.azure.pathogenesis.entity.ai.common.CortexGlue;
import com.azure.pathogenesis.entity.ai.common.PathogenAction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class SeekCoverAction extends PathogenAction<BloodbursterEntity, BloodbursterGoal> {

    private int ticks;

    public SeekCoverAction(int priority) {
        super("bloodburster_seek_cover", priority);
    }

    @Override
    public void start(BloodbursterEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        var cover = findCover(agent);
        if (cover != null) {
            agent.getNavigation().moveTo(cover.x, cover.y, cover.z, 1.25D);
        }
    }

    private static Vec3 findCover(BloodbursterEntity agent) {
        for (var i = 0; i < 10; i++) {
            var candidate = DefaultRandomPos.getPos(agent, 10, 4);
            if (candidate != null && BloodbursterEntity.isCovered(agent.level(), BlockPos.containing(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public ActionOutcome<BloodbursterGoal> tick(
        BloodbursterEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns
    ) {
        if (!agent.isExposed()) {
            return CortexGlue.done();
        }
        if (++ticks > 200) {
            return CortexGlue.failed(PlanFailureReason.FAILED_UNSUITABLE_CONDITIONS, agent.blockPosition());
        }
        if (agent.getNavigation().isDone()) {
            var cover = findCover(agent);
            if (cover == null) {
                return CortexGlue.failed(PlanFailureReason.FAILED_NO_VALID_PLACEMENT, agent.blockPosition());
            }
            agent.getNavigation().moveTo(cover.x, cover.y, cover.z, 1.25D);
        }
        return CortexGlue.running();
    }
}
