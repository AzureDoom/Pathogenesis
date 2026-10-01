package com.azure.pathogenesis.infection;

import com.azure.pathogenesis.exposure.PathogenExposure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class HostState {

    @Nullable
    private PathogenExposure exposure;

    @Nullable
    private NeomorphInfection infection;

    BlockPos lastKnownPos = BlockPos.ZERO;

    @Nullable
    ResourceKey<Level> dimension;

    boolean isPlayer;

    @Nullable
    public PathogenExposure exposure() {
        return exposure;
    }

    public PathogenExposure getOrCreateExposure() {
        if (exposure == null) {
            exposure = new PathogenExposure();
        }
        return exposure;
    }

    public void clearExposure() {
        exposure = null;
    }

    @Nullable
    public NeomorphInfection infection() {
        return infection;
    }

    public void setInfection(@Nullable NeomorphInfection infection) {
        this.infection = infection;
    }

    public boolean isEmpty() {
        return exposure == null && infection == null;
    }

    void setExposure(@Nullable PathogenExposure exposure) {
        this.exposure = exposure;
    }
}
