package com.azure.pathogenesis.entity.ai.hammerpede;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.azurecortex.runtime.InterruptCategory;
import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.entity.HammerpedeEntity;
import com.azure.pathogenesis.entity.ai.common.CortexGlue;
import com.azure.pathogenesis.entity.ai.common.PathogenAction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class LungeAction extends PathogenAction<HammerpedeEntity, HammerpedeGoal> {

    public static final String COOLDOWN = "hammerpede_lunge_cd";

    private static final int WINDUP = 6;

    private static final double MIN_RANGE = 2.5D;

    private int ticks;

    private boolean launched;

    private boolean hit;

    public LungeAction(int priority) {
        super("hammerpede_lunge", priority, InterruptCategory.LOCKED);
    }

    public boolean canStart(HammerpedeEntity agent, LivingEntity target) {
        var range = Pathogenesis.getConfig().entityConfigs.hammerpedeConfigs.hammerpedeLungeRange;
        var distanceSqr = agent.distanceToSqr(target);
        return agent.onGround() && distanceSqr >= MIN_RANGE * MIN_RANGE && distanceSqr <= range * range
            && Math.abs(target.getY() - agent.getY()) <= 2.5D && agent.hasLineOfSight(target);
    }

    @Override
    public void start(HammerpedeEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        launched = false;
        hit = false;
        agent.getNavigation().stop();
        agent.onLungeWindup();
    }

    @Override
    public ActionOutcome<HammerpedeGoal> tick(
        HammerpedeEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns
    ) {
        var target = blackboard.get(CommonBlackboardKeys.TARGET);
        ticks++;
        if (!launched) {
            if (target == null || !target.isAlive()) {
                return CortexGlue.failed(PlanFailureReason.FAILED_TARGET_LOST, agent.blockPosition());
            }
            agent.getLookControl().setLookAt(target, 90.0F, 90.0F);
            if (ticks < WINDUP) {
                return CortexGlue.running();
            }
            var delta = target.position().subtract(agent.position());
            var horizontal = new Vec3(delta.x, 0.0D, delta.z);
            var velocity = horizontal.normalize()
                .scale(Math.min(1.3D, 0.4D + horizontal.length() * 0.15D))
                .add(0.0D, 0.22D + Math.max(0.0D, delta.y) * 0.1D, 0.0D);
            agent.setDeltaMovement(velocity);
            agent.hasImpulse = true;
            agent.onLungeLaunch();
            launched = true;
            return CortexGlue.running();
        }
        if (
            !hit && target != null && target.isAlive() && agent.getBoundingBox()
                .inflate(0.35D)
                .intersects(target.getBoundingBox())
        ) {
            hit = true;
            agent.doHurtTarget(target);
        }
        if (agent.onGround() && ticks > WINDUP + 4 || ticks > WINDUP + 24) {
            return CortexGlue.done();
        }
        return CortexGlue.running();
    }

    @Override
    public void stop(
        HammerpedeEntity agent,
        Blackboard blackboard,
        CooldownTracker cooldowns,
        ActionStatus reason
    ) {
        super.stop(agent, blackboard, cooldowns, reason);
        cooldowns.set(COOLDOWN, Pathogenesis.getConfig().entityConfigs.hammerpedeConfigs.hammerpedeLungeCooldown);
    }
}
