package com.azure.pathogenesis.entity.ai.neomorph;

import com.azure.azurecortex.api.goal.Goal;

public enum NeomorphGoal implements Goal {

    NONE,
    ROAM,
    INVESTIGATE,
    INVESTIGATE_SOUND,
    HUNT,
    RETREAT;

    @Override
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isPassive() {
        return this == NONE || this == ROAM || this == INVESTIGATE_SOUND;
    }
}
