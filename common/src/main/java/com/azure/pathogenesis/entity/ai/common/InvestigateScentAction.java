package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.pathogenesis.contamination.CarcassSites;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

public class InvestigateScentAction<E extends PathfinderMob & ScentFollower, G> extends PathogenAction<E, G> {

    private static final int SNIFF_TICKS = 60;

    private static final int TIMEOUT_TICKS = 300;

    private static final int SITE_CHECK_INTERVAL = 20;

    private static final double ARRIVE_DISTANCE_SQR = 4.0D;

    private final double speed;

    private BlockPos destination;

    private int ticks;

    private int sniffTicks = -1;

    public InvestigateScentAction(String name, int priority, double speed) {
        super(name, priority);
        this.speed = speed;
    }

    @Override
    public void start(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        destination = agent.scent().target();
        ticks = 0;
        sniffTicks = -1;
        if (destination != null) {
            agent.getNavigation()
                .moveTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D, speed);
        }
    }

    @Override
    public ActionOutcome<G> tick(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        if (destination == null || !(agent.level() instanceof ServerLevel level)) {
            return CortexGlue.failed(PlanFailureReason.FAILED_PRECONDITION, agent.blockPosition());
        }
        var now = level.getGameTime();
        ticks++;
        if (ticks % SITE_CHECK_INTERVAL == 0 && !CarcassSites.hasSiteAt(level, destination)) {
            agent.scent().markInvestigated(now);
            return CortexGlue.done();
        }

        if (sniffTicks >= 0) {
            if (sniffTicks++ % 15 == 0) {
                var random = agent.getRandom();
                agent.getLookControl()
                    .setLookAt(
                        destination.getX() + 0.5D + random.nextGaussian() * 0.8D,
                        destination.getY(),
                        destination.getZ() + 0.5D + random.nextGaussian() * 0.8D
                    );
            }
            if (sniffTicks > SNIFF_TICKS) {
                agent.scent().markInvestigated(now);
                return CortexGlue.done();
            }
            return CortexGlue.running();
        }

        if (agent.blockPosition().distSqr(destination) <= ARRIVE_DISTANCE_SQR) {
            agent.getNavigation().stop();
            agent.playAmbientSound();
            sniffTicks = 0;
            return CortexGlue.running();
        }
        if (ticks > TIMEOUT_TICKS || agent.getNavigation().isDone() && ticks > 20) {
            agent.scent().markInvestigated(now);
            return CortexGlue.failed(PlanFailureReason.FAILED_STUCK, agent.blockPosition());
        }
        return CortexGlue.running();
    }
}
