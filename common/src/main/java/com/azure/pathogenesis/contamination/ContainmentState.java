package com.azure.pathogenesis.contamination;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum ContainmentState implements StringRepresentable {

    /** Intact and safe. */
    SEALED,
    /** Cracked; will begin leaking on its own shortly, or immediately if struck again. */
    DAMAGED,
    /** Releasing Pathogen; contamination zone active. */
    LEAKING,
    /** Fully breached; faster drain, stronger exposure. */
    OPEN,
    /** Spent. Inert, but whatever it contaminated remains. */
    EMPTY;

    public boolean isLeaking() {
        return this == LEAKING || this == OPEN;
    }

    public boolean needsTicking() {
        return this == DAMAGED || this == LEAKING || this == OPEN;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
