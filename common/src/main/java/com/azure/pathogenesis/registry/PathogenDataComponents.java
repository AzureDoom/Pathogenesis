package com.azure.pathogenesis.registry;

import com.azure.pathogenesis.item.sampler.SampleResult;
import com.azure.pathogenesis.platform.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import java.util.function.Supplier;

public final class PathogenDataComponents {

    public static final Supplier<DataComponentType<SampleResult>> SAMPLE_RESULT = Services.REGISTRY
        .register(
            Registries.DATA_COMPONENT_TYPE,
            "sample_result",
            () -> DataComponentType.<SampleResult>builder()
                .persistent(SampleResult.CODEC)
                .networkSynchronized(SampleResult.STREAM_CODEC)
                .build()
        );

    private PathogenDataComponents() {}

    public static void init() {}
}
