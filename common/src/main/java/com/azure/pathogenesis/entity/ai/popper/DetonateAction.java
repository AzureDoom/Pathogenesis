package com.azure.pathogenesis.entity.ai.popper;

import com.azure.azurecortex.api.action.ActionOutcome;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.azurecortex.runtime.InterruptCategory;
import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.entity.PopperEntity;
import com.azure.pathogenesis.entity.ai.common.CortexGlue;
import com.azure.pathogenesis.entity.ai.common.PathogenAction;
import net.minecraft.world.entity.LivingEntity;

public class DetonateAction extends PathogenAction<PopperEntity, PopperGoal> {

    public DetonateAction(int priority) {
        super("popper_detonate", priority, InterruptCategory.LOCKED);
    }

    public boolean canStart(PopperEntity agent, LivingEntity target) {
        var range = Pathogenesis.getConfig().entityConfigs.popperConfigs.popperTriggerRange
            + target.getBbWidth() * 0.5D;
        return agent.distanceToSqr(target) <= range * range && agent.hasLineOfSight(target);
    }

    @Override
    public void start(PopperEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        agent.getNavigation().stop();
        agent.startFuse();
    }

    @Override
    public ActionOutcome<PopperGoal> tick(PopperEntity agent, Blackboard blackboard, CooldownTracker cooldowns) {
        agent.getNavigation().stop();
        var target = blackboard.get(CommonBlackboardKeys.TARGET);
        if (target != null) {
            agent.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        return agent.isFusing() ? CortexGlue.running() : CortexGlue.done();
    }
}
