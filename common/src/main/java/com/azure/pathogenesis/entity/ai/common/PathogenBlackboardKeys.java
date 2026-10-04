package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.blackboard.BlackboardKey;
import net.minecraft.world.entity.LivingEntity;

public final class PathogenBlackboardKeys {

    public static final BlackboardKey<LivingEntity> THREAT = BlackboardKey.of(
        "pathogenesis_threat",
        LivingEntity.class
    );

    public static final String NEOMORPH_MELEE_COOLDOWN = "neomorph_melee_cd";

    public static final String COOLDOWN = "neomorph_leap_cd";

    public static final String RETREAT_COOLDOWN = "neomorph_retreat_cd";

    public static final String RETREAT_COOLDOWN_NEOPHYTE = "neophyte_retreat_cd";

    public static final Float RETREAT_HEALTH = 0.50F;

    public static final Float RETREAT_HEALTH_NEOMORPH = 0.30F;

    private PathogenBlackboardKeys() {}
}
