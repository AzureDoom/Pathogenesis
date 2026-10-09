package com.azure.pathogenesis.entity.ai.bloodburster;

import com.azure.pathogenesis.entity.ai.common.PathogenGoal;

public enum BloodbursterGoal implements PathogenGoal {

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

    @Override
    public boolean isPassive() {
        return this == NONE || this == WANDER || this == INVESTIGATE_SCENT;
    }
}
