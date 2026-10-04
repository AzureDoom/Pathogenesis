package com.azure.pathogenesis.entity.ai.bloodburster;

import com.azure.azurecortex.api.goal.Goal;

public enum BloodbursterGoal implements Goal {

    NONE,
    WANDER,
    INVESTIGATE_SCENT,
    SEEK_COVER,
    FEED,
    FLEE;

    @Override
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isPassive() {
        return this == NONE || this == WANDER || this == INVESTIGATE_SCENT;
    }
}
