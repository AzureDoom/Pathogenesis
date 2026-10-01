package com.azure.pathogenesis.entity.ai.bloodburster;

import com.azure.azurecortex.api.goal.Goal;

public enum BloodbursterGoal implements Goal {

    NONE,
    WANDER,
    SEEK_COVER,
    FEED,
    FLEE;

    @Override
    public boolean isNone() {
        return this == NONE;
    }
}
