package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class RoamAction<E extends PathfinderMob, G> extends PathogenAction<E, G> {

    private final double speed;

    private final int horizontalRange;

    private final int maxDuration;

    private int ticks;

    private int pause;

    public RoamAction(String name, int priority, double speed, int horizontalRange, int maxDuration) {
        super(name, priority);
        this.speed = speed;
        this.horizontalRange = horizontalRange;
        this.maxDuration = maxDuration;
    }

    @Override
    public void start(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        pause = 0;
        pickDestination(agent);
    }

    protected Vec3 findDestination(E agent) {
        return DefaultRandomPos.getPos(agent, horizontalRange, 7);
    }

    private boolean pickDestination(E agent) {
        var destination = findDestination(agent);
        return destination != null && agent.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
    }

    @Override
    public ActionOutcome<G> tick(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (++ticks > maxDuration) {
            return CortexGlue.done();
        }
        if (agent.getNavigation().isDone()) {
            if (pause <= 0) {
                pause = 40 + agent.getRandom().nextInt(80);
            } else if (--pause <= 0 && !pickDestination(agent) && ticks > maxDuration / 2) {
                return CortexGlue.failed(PlanFailureReason.FAILED_NO_PATH, agent.blockPosition());
            }
        }
        return CortexGlue.running();
    }
}
