package com.azure.pathogenesis.entity.ai.neophyte;

import com.azure.pathogenesis.entity.ai.common.PathogenGoal;

public enum NeophyteGoal implements PathogenGoal {

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

    @Override
    public boolean isPassive() {
        return this == NONE || this == ROAM || this == INVESTIGATE_SOUND || this == INVESTIGATE_SCENT;
    }
}
