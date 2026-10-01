package com.azure.pathogenesis.entity.ai.neomorph;

import com.azure.azurecortex.api.action.Action;
import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.pathogenesis.entity.ai.common.SoundListener;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;

public class InvestigateSoundAction<E extends PathfinderMob & SoundListener, G> implements Action<E, G> {

    private static final int LOOK_AROUND_TICKS = 40;

    private final String name;

    private final int priority;

    private BlockPos destination;

    private int ticks;

    private int arrivedTicks = -1;

    public InvestigateSoundAction(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    @Override
    public void start(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        destination = agent.heardPos();
        ticks = 0;
        arrivedTicks = -1;
        if (destination != null) {
            agent.getNavigation()
                .moveTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D, 1.0D);
        }
    }

    @Override
    public ActionOutcome<G> tick(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (destination == null) {
            return ActionOutcome.failed(PlanFailureReason.FAILED_PRECONDITION, agent.blockPosition());
        }
        var latest = agent.heardPos();
        if (latest != null && !latest.equals(destination)) {
            start(agent, blackboard, cooldowns);
            return ActionOutcome.running();
        }
        if (arrivedTicks >= 0) {
            if (++arrivedTicks % 10 == 0) {
                agent.getLookControl()
                    .setLookAt(
                        agent.getX() + agent.getRandom().nextGaussian() * 4.0D,
                        agent.getEyeY(),
                        agent.getZ() + agent.getRandom().nextGaussian() * 4.0D
                    );
            }
            if (arrivedTicks > LOOK_AROUND_TICKS) {
                agent.clearHeard();
                return ActionOutcome.success();
            }
            return ActionOutcome.running();
        }
        if (agent.blockPosition().distSqr(destination) <= 4.0D) {
            agent.getNavigation().stop();
            arrivedTicks = 0;
            return ActionOutcome.running();
        }
        if (++ticks > 240 || agent.getNavigation().isDone() && ticks > 20) {
            agent.clearHeard();
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
        return name;
    }
}
