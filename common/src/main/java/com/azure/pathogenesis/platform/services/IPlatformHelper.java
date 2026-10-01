package com.azure.pathogenesis.platform.services;

import com.azure.pathogenesis.fluid.ContaminatedWaterFluid;
import net.minecraft.world.item.CreativeModeTab;

@SuppressWarnings("unused")
public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    CreativeModeTab.Builder creativeTabBuilder();

    ContaminatedWaterFluid.Source createContaminatedWaterSource();

    ContaminatedWaterFluid.Flowing createContaminatedWaterFlowing();
}
