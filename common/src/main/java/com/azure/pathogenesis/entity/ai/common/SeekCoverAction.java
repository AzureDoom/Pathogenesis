package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.action.Action;
import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class SeekCoverAction<E extends PathfinderMob, G> implements Action<E, G> {

    private final double speed;

    private final int radius;

    private final int priority;

    @Nullable
    private Vec3 cover;

    private int ticks;

    public SeekCoverAction(double speed, int radius, int priority) {
        this.speed = speed;
        this.radius = radius;
        this.priority = priority;
    }

    @Override
    public void start(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        cover = null;
        var bestSky = Integer.MAX_VALUE;
        for (var i = 0; i < 12; i++) {
            var candidate = LandRandomPos.getPos(agent, radius, 4);
            if (candidate == null) {
                continue;
            }
            var pos = BlockPos.containing(candidate);
            var sky = agent.level().canSeeSky(pos) ? 15 : agent.level().getBrightness(LightLayer.SKY, pos);
            if (sky < bestSky) {
                bestSky = sky;
                cover = candidate;
            }
        }
        if (cover != null) {
            agent.getNavigation().moveTo(cover.x, cover.y, cover.z, speed);
        }
    }

    @Override
    public ActionOutcome<G> tick(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (cover == null) {
            return ActionOutcome.failed(PlanFailureReason.FAILED_NO_VALID_PLACEMENT, agent.blockPosition());
        }
        if (agent.position().distanceToSqr(cover) < 2.25D) {
            return ActionOutcome.success();
        }
        if (++ticks > 200 || agent.getNavigation().isDone() && ticks > 10) {
            return ActionOutcome.failed(PlanFailureReason.FAILED_STUCK, agent.blockPosition());
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(E agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
    }

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public String debugName() {
        return "seek_cover";
    }
}
