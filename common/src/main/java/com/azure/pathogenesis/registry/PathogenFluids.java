package com.azure.pathogenesis.registry;

import com.azure.pathogenesis.fluid.ContaminatedWaterFluid;
import com.azure.pathogenesis.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Supplier;

public final class PathogenFluids {

    public static final Supplier<ContaminatedWaterFluid.Source> CONTAMINATED_WATER = register(
        "contaminated_water",
        Services.PLATFORM::createContaminatedWaterSource
    );

    public static final Supplier<ContaminatedWaterFluid.Flowing> FLOWING_CONTAMINATED_WATER = register(
        "flowing_contaminated_water",
        Services.PLATFORM::createContaminatedWaterFlowing
    );

    private PathogenFluids() {}

    private static <F extends Fluid> Supplier<F> register(String name, Supplier<F> factory) {
        return Services.REGISTRY.register(Registries.FLUID, name, factory);
    }

    public static void init() {}
}
