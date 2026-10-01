package com.azure.pathogenesis.fabric.platform;

import com.azure.pathogenesis.fluid.ContaminatedWaterFluid;
import com.azure.pathogenesis.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;

public final class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
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
