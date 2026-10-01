package com.azure.pathogenesis.registry;

import com.azure.pathogenesis.blockentity.PathogenSourceBlockEntity;
import com.azure.pathogenesis.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class PathogenBlockEntities {

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<PathogenSourceBlockEntity>> PATHOGEN_SOURCE = Services.REGISTRY
        .register(
            Registries.BLOCK_ENTITY_TYPE,
            "pathogen_source",
            () -> BlockEntityType.Builder.of(PathogenSourceBlockEntity::new, PathogenBlocks.PATHOGEN_SOURCE.get())
                .build(null)
        );

    private PathogenBlockEntities() {}

    public static void init() {}
}
