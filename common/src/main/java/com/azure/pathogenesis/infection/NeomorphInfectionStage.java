package com.azure.pathogenesis.infection;

public enum NeomorphInfectionStage {

    EXPOSED(0.0F),
    INCUBATING(0.10F),
    SYMPTOMATIC(0.55F),
    TERMINAL(1.0F);

    public final float startsAt;

    NeomorphInfectionStage(float startsAt) {
        this.startsAt = startsAt;
    }

    public static NeomorphInfectionStage forProgress(float progress) {
        if (progress >= TERMINAL.startsAt) {
            return TERMINAL;
        }
        if (progress >= SYMPTOMATIC.startsAt) {
            return SYMPTOMATIC;
        }
        if (progress >= INCUBATING.startsAt) {
            return INCUBATING;
        }
        return EXPOSED;
    }

    public boolean burstsOnDeath() {
        return ordinal() >= SYMPTOMATIC.ordinal();
    }
}
