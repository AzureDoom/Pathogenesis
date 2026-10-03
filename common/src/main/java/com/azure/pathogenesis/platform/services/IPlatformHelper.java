package com.azure.pathogenesis.platform.services;

import com.azure.pathogenesis.fluid.ContaminatedWaterFluid;

public interface IPlatformHelper {

    boolean isModLoaded(String modId);

    ContaminatedWaterFluid.Source createContaminatedWaterSource();

    ContaminatedWaterFluid.Flowing createContaminatedWaterFlowing();
}
