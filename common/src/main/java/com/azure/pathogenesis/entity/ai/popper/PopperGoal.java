package com.azure.pathogenesis.entity.ai.popper;

import com.azure.azurecortex.api.goal.Goal;

public enum PopperGoal implements Goal {

    NONE,
    DRIFT,
    APPROACH;

    @Override
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isPassive() {
        return this != APPROACH;
    }
}
