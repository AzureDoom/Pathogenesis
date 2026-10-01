package com.azure.pathogenesis.contamination;

public enum RuptureStrength {

    /** A cracked canister finally giving way. */
    CRACK(1, 2.0D, 25),
    /** Broken by hand, or an ampule item destroyed. */
    RUPTURE(3, 4.0D, 70),
    /** Blown apart. */
    EXPLOSION(4, 6.0D, 90);

    public final int burstRadius;

    public final double exposureRadius;

    public final int exposure;

    RuptureStrength(int burstRadius, double exposureRadius, int exposure) {
        this.burstRadius = burstRadius;
        this.exposureRadius = exposureRadius;
        this.exposure = exposure;
    }
}
