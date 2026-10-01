package com.azure.pathogenesis.entity.ai.common;

@SuppressWarnings("unused")
public final class PathogenPriorities {

    public static final int IDLE = 0;

    public static final int WANDER = 5;

    public static final int INVESTIGATE = 8;

    public static final int INVESTIGATE_SOUND = 9;

    public static final int SEEK_COVER = 15;

    public static final int CHASE = 20;

    public static final int STALK = 22;

    public static final int MELEE = 30;

    public static final int LEAP = 40;

    public static final int FLEE = 60;

    private PathogenPriorities() {}
}
