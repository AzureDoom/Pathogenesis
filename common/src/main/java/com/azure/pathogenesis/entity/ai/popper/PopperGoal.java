package com.azure.pathogenesis.entity.ai.popper;

import com.azure.pathogenesis.entity.ai.common.PathogenGoal;

public enum PopperGoal implements PathogenGoal {

    NONE,
    DRIFT,
    APPROACH;

    @Override
    public boolean isNone() {
        return this == NONE;
    }

    @Override
    public boolean isPassive() {
        return this != APPROACH;
    }
}
