package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.PlanFailureReason;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.azurecortex.runtime.InterruptCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.function.BiConsumer;

public class WindupMeleeAction<E extends PathfinderMob, G> extends PathogenAction<E, G> {

    private final int windup;

    private final double reach;

    private final String cooldownKey;

    private final int cooldown;

    private final BiConsumer<E, LivingEntity> onStart;

    private int ticks;

    public WindupMeleeAction(
        String name,
        int priority,
        int windup,
        double reach,
        String cooldownKey,
        int cooldown,
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
        double r = reach + agent.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D;
        return agent.distanceToSqr(target) <= r * r;
    }

    @Override
    public void start(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        ticks = 0;
        agent.getNavigation().stop();
        var target = blackboard.get(CommonBlackboardKeys.TARGET);
        if (target != null) {
            onStart.accept(agent, target);
        }
    }

    @Override
    public ActionOutcome<G> tick(E agent, Blackboard blackboard, CooldownTracker cooldowns) {
        var target = blackboard.get(CommonBlackboardKeys.TARGET);
        if (target == null || !target.isAlive()) {
            cooldowns.set(cooldownKey, cooldown / 2);
            return CortexGlue.failed(PlanFailureReason.FAILED_TARGET_LOST, agent.blockPosition());
        }
        agent.getLookControl().setLookAt(target, 60.0F, 60.0F);
        if (++ticks < windup) {
            return CortexGlue.running();
        }
        cooldowns.set(cooldownKey, cooldown);
        if (inReach(agent, target) && agent.hasLineOfSight(target)) {
            agent.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            agent.doHurtTarget(target);
        }
        return CortexGlue.done();
    }
}
