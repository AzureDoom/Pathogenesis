package com.azure.pathogenesis.entity.ai.fauna;

public enum FaunaSeverity {

    CLEAN,
    /** Stops breeding, wanders erratically, shuns clean animals, coughs. */
    MODERATE,
    /** Panics (toward or away from the zone, by species) or turns aggressive. */
    HIGH,
    /** Convulses and dies. */
    EXTREME;

    public boolean atLeast(FaunaSeverity other) {
        return ordinal() >= other.ordinal();
    }

    public FaunaSeverity max(FaunaSeverity other) {
        return other.ordinal() > ordinal() ? other : this;
    }
}
