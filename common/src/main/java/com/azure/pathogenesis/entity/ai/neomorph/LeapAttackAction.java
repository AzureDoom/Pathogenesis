package com.azure.pathogenesis.entity.ai.neomorph;

import com.azure.azurecortex.api.action.Action;
import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.azurecortex.runtime.InterruptCategory;
import com.azure.pathogenesis.entity.NeomorphEntity;
import net.minecraft.world.phys.Vec3;

public class LeapAttackAction implements Action<NeomorphEntity, NeomorphGoal> {

    public static final String COOLDOWN = "neomorph_leap_cd";

    public static final int COOLDOWN_TICKS = 80;

    private static final int CROUCH_TICKS = 8;

    private static final int MAX_FLIGHT_TICKS = 30;

    private final int priority;

    private int ticks;

    private boolean launched;

    private boolean hit;

    public LeapAttackAction(int priority) {
        this.priority = priority;
    }

    @Override
    public void start(NeomorphEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        launched = false;
        hit = false;
        agent.getNavigation().stop();
        agent.onLeapWindup();
    }

    @Override
    public ActionOutcome<NeomorphGoal> tick(NeomorphEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = blackboard.get(CommonBlackboardKeys.TARGET);
        ticks++;
        if (!launched) {
            if (target == null || !target.isAlive()) {
                return ActionOutcome.failed(PlanFailureReason.FAILED_TARGET_LOST, agent.blockPosition());
            }
            agent.getLookControl().setLookAt(target, 90.0F, 90.0F);
            if (ticks < CROUCH_TICKS) {
                return ActionOutcome.running();
            }
            var delta = target.position().subtract(agent.position());
            var horizontal = new Vec3(delta.x, 0.0D, delta.z);
            var distance = horizontal.length();
            var velocity = horizontal.normalize()
                .scale(Math.min(1.6D, 0.35D + distance * 0.16D))
                .add(0.0D, 0.42D + Math.max(0.0D, delta.y) * 0.08D, 0.0D);
            agent.setDeltaMovement(velocity);
            agent.hasImpulse = true;
            agent.onLeapLaunch();
            launched = true;
            return ActionOutcome.running();
        }
        if (
            !hit && target != null && target.isAlive() && agent.getBoundingBox()
                .inflate(0.4D)
                .intersects(target.getBoundingBox())
        ) {
            hit = true;
            agent.doHurtTarget(target);
        }
        if (agent.onGround() && ticks > CROUCH_TICKS + 4 || ticks > CROUCH_TICKS + MAX_FLIGHT_TICKS) {
            return ActionOutcome.success();
        }
        return ActionOutcome.running();
    }

    @Override
    public void stop(NeomorphEntity agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        cooldowns.set(COOLDOWN, COOLDOWN_TICKS);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public InterruptCategory interruptCategory() {
        return InterruptCategory.LOCKED;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public String debugName() {
        return "neomorph_leap";
    }
}
