package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.azurecortex.runtime.InterruptCategory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

public class WindupMeleeAction<E extends PathfinderMob, G> extends PathogenAction<E, G> {

    private final IntSupplier windup;

    private final double reach;

    private final String cooldownKey;

    private final IntSupplier cooldown;

    private final BiConsumer<E, LivingEntity> onStart;

    private int ticks;

    public WindupMeleeAction(
        String name,
        int priority,
        IntSupplier windup,
        double reach,
        String cooldownKey,
        IntSupplier cooldown,
        BiConsumer<E, LivingEntity> onStart
    ) {
        super(name, priority, InterruptCategory.LOCKED);
        this.windup = windup;
        this.reach = reach;
        this.cooldownKey = cooldownKey;
        this.cooldown = cooldown;
        this.onStart = onStart;
    }

    public String cooldownKey() {
        return cooldownKey;
    }

    public boolean inReach(E agent, LivingEntity target) {
        return inReach(agent, target, 0.0D);
    }

    public boolean canStart(E agent, LivingEntity target) {
        return inReach(agent, target, 0.75D);
    }

    private boolean inReach(E agent, LivingEntity target, double extra) {
        double r = reach + extra + agent.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D;
        return agent.distanceToSqr(target) <= r * r;
    }

    @Override
    public void start(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        var target = blackboard.get(CommonBlackboardKeys.TARGET);
        if (target != null) {
            onStart.accept(agent, target);
        }
    }

    @Override
    public ActionOutcome<G> tick(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = blackboard.get(CommonBlackboardKeys.TARGET);
        if (target == null || !target.isAlive()) {
            cooldowns.set(cooldownKey, missCooldown());
            return CortexGlue.failed(PlanFailureReason.FAILED_TARGET_LOST, agent.blockPosition());
        }
        agent.getLookControl().setLookAt(target, 60.0F, 60.0F);
        if (!inReach(agent, target)) {
            agent.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.2D);
        }
        if (++ticks < windup.getAsInt()) {
            return CortexGlue.running();
        }
        if (inReach(agent, target, 0.35D) && agent.hasLineOfSight(target)) {
            agent.swing(InteractionHand.MAIN_HAND);
            agent.doHurtTarget(target);
            cooldowns.set(cooldownKey, cooldown.getAsInt());
        } else {
            cooldowns.set(cooldownKey, missCooldown());
        }
        return CortexGlue.done();
    }

    private int missCooldown() {
        return Math.max(4, cooldown.getAsInt() / 3);
    }
}
