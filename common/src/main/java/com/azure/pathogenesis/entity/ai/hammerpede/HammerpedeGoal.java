package com.azure.pathogenesis.entity.ai.hammerpede;

import com.azure.pathogenesis.entity.ai.common.PathogenGoal;

public enum HammerpedeGoal implements PathogenGoal {

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

    @Override
    public boolean isPassive() {
        return this == NONE || this == LURK || this == HIDE || this == WANDER || this == INVESTIGATE_SOUND;
    }
}
