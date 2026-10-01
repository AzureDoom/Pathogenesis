package com.azure.pathogenesis.entity.ai.common;

import com.azure.azurecortex.api.blackboard.BlackboardKey;
import net.minecraft.world.entity.LivingEntity;

public final class PathogenBlackboardKeys {

    public static final BlackboardKey<LivingEntity> THREAT = BlackboardKey.of(
        "pathogenesis_threat",
        LivingEntity.class
    );

    private PathogenBlackboardKeys() {}
}
