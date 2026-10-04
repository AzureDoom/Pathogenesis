package com.azure.pathogenesis.entity.ai.neophyte;

import com.azure.azurecortex.api.goal.Goal;

public enum NeophyteGoal implements Goal {

    NONE,
    ROAM,
    INVESTIGATE,
    INVESTIGATE_SOUND,
    INVESTIGATE_SCENT,
    HUNT,
    RETREAT;

    @Override
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isPassive() {
        return this == NONE || this == ROAM || this == INVESTIGATE_SOUND || this == INVESTIGATE_SCENT;
    }
}
