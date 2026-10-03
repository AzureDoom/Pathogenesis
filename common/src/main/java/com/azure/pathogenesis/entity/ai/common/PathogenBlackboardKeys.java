package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.blackboard.BlackboardKey;
import net.minecraft.world.entity.LivingEntity;

public final class PathogenBlackboardKeys {

    public static final BlackboardKey<LivingEntity> THREAT = BlackboardKey.of(
        "pathogenesis_threat",
        LivingEntity.class
    );

    public static final String COOLDOWN = "neomorph_leap_cd";

    public static final String RETREAT_COOLDOWN = "neomorph_retreat_cd";

    private PathogenBlackboardKeys() {}
}
