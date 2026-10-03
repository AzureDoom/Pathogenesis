package com.azure.pathogenesis.fabric.platform;

import com.azure.pathogenesis.fluid.ContaminatedWaterFluid;
import com.azure.pathogenesis.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public ContaminatedWaterFluid.Source createContaminatedWaterSource() {
        return new ContaminatedWaterFluid.Source();
    }

    @Override
    public ContaminatedWaterFluid.Flowing createContaminatedWaterFlowing() {
        return new ContaminatedWaterFluid.Flowing();
    }
}
