package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.action.Action;
import com.azure.azurecortex.api.action.ActionStatus;
import com.azure.azurecortex.api.blackboard.Blackboard;
import com.azure.azurecortex.runtime.CooldownTracker;
import com.azure.azurecortex.runtime.InterruptCategory;
import net.minecraft.world.entity.PathfinderMob;

public abstract class PathogenAction<E extends PathfinderMob, G> implements Action<E, G> {

    private final String name;

    private final int priority;

    private final InterruptCategory category;

    protected PathogenAction(String name, int priority, InterruptCategory category) {
        this.name = name;
        this.priority = priority;
        this.category = category;
    }

    protected PathogenAction(String name, int priority) {
        this(name, priority, InterruptCategory.NORMAL);
    }

    @Override
    public void start(E agent, Blackboard blackboard, CooldownTracker cooldowns) {}

    @Override
    public void stop(E agent, Blackboard blackboard, CooldownTracker cooldowns, ActionStatus reason) {
        agent.getNavigation().stop();
    }

    @Override
    public boolean isInterruptible() {
        return category == InterruptCategory.NORMAL;
    }

    @Override
    public InterruptCategory interruptCategory() {
        return category;
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
