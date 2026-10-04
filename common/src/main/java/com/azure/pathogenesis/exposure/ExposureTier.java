package com.azure.pathogenesis.exposure;

import java.util.Locale;

public enum ExposureTier {

    NONE(0),
    LOW(20),
    MODERATE(55),
    HIGH(110),
    EXTREME(180);

    private static final ExposureTier[] DESCENDING = { EXTREME, HIGH, MODERATE, LOW };

    public final int threshold;

    ExposureTier(int threshold) {
        this.threshold = threshold;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ExposureTier of(int dose) {
        for (var tier : DESCENDING) {
            if (dose >= tier.threshold) {
                return tier;
            }
        }
        return NONE;
    }
}
