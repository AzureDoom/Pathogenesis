package com.azure.pathogenesis.neoforge.platform;

import com.azure.pathogenesis.fluid.ContaminatedWaterFluid;
import com.azure.pathogenesis.neoforge.PathogenFluidTypes;
import com.azure.pathogenesis.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;

public final class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public ContaminatedWaterFluid.Source createContaminatedWaterSource() {
        return new ContaminatedWaterFluid.Source() {

            @Override
            public @NotNull FluidType getFluidType() {
                return PathogenFluidTypes.CONTAMINATED_WATER.get();
            }
        };
    }

    @Override
    public ContaminatedWaterFluid.Flowing createContaminatedWaterFlowing() {
        return new ContaminatedWaterFluid.Flowing() {

            @Override
            public @NotNull FluidType getFluidType() {
                return PathogenFluidTypes.CONTAMINATED_WATER.get();
            }
        };
    }
}
