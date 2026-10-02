package com.azure.pathogenesis.registry;

import com.azure.pathogenesis.platform.Services;
import com.azure.pathogenesis.worldgen.DryLandStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.function.Supplier;

public final class PathogenStructureTypes {

    public static final Supplier<StructureType<DryLandStructure>> DRY_LAND = Services.REGISTRY.register(
        Registries.STRUCTURE_TYPE,
        "dry_land",
        () -> () -> DryLandStructure.CODEC
    );

    private PathogenStructureTypes() {}

    public static void init() {}
}
