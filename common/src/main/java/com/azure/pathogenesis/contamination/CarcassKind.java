package com.azure.pathogenesis.contamination;

import java.util.Locale;

public enum CarcassKind {

    /** Remains left by a Bloodburster or Neophyte feeding. Contaminates only inside an existing zone. */
    FEEDING(1, 0.30F, 0, 0.0D, 0, 1800, 0.05F, 0.35F),
    /** Highly exposed, or merely died inside an established zone. Contaminates only inside an existing zone. */
    MINOR(1, 0.35F, 10, 2.0D, 1, 1200, 0.00F, 0.25F),
    /** Infected, pre-symptomatic. Will seed a new zone if there isn't one. */
    INFECTED(2, 0.50F, 25, 3.0D, 2, 2400, 0.10F, 0.60F),
    /** Symptomatic/terminal host or extreme exposure. Will seed a new zone if there isn't one. */
    VIRULENT(3, 0.65F, 45, 4.0D, 3, 3600, 0.35F, 0.90F);

    public final int burstRadius;

    public final float convertChance;

    public final int burstExposure;

    public final double burstExposureRadius;

    public final int lingerExposure;

    public final int duration;

    public final float sporeChance;

    public final float growthChance;

    CarcassKind(
        int burstRadius,
        float convertChance,
        int burstExposure,
        double burstExposureRadius,
        int lingerExposure,
        int duration,
        float sporeChance,
        float growthChance
    ) {
        this.burstRadius = burstRadius;
        this.convertChance = convertChance;
        this.burstExposure = burstExposure;
        this.burstExposureRadius = burstExposureRadius;
        this.lingerExposure = lingerExposure;
        this.duration = duration;
        this.sporeChance = sporeChance;
        this.growthChance = growthChance;
    }

    public boolean seedsZone() {
        return this == INFECTED || this == VIRULENT;
    }

    public CarcassKind strongest(CarcassKind other) {
        return other.ordinal() > ordinal() ? other : this;
    }

    public CarcassKind stronger() {
        return this == VIRULENT ? VIRULENT : values()[Math.max(ordinal() + 1, MINOR.ordinal())];
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CarcassKind byId(String id) {
        for (var kind : values()) {
            if (kind.id().equals(id)) {
                return kind;
            }
        }
        return MINOR;
    }
}
