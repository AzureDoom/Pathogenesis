package com.azure.pathogenesis.entity.ai.hammerpede;

import com.azure.azurecortex.api.goal.Goal;

public enum HammerpedeGoal implements Goal {

    NONE,
    LURK,
    HIDE,
    WANDER,
    INVESTIGATE_SOUND,
    STRIKE,
    RETREAT;

    @Override
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isPassive() {
        return this == NONE || this == LURK || this == HIDE || this == WANDER || this == INVESTIGATE_SOUND;
    }
}
