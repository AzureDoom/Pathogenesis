package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.config.PathogenesisConfig;

import java.util.Locale;

public enum OutbreakPhase {

    SOURCE_FED,
    ECOLOGICAL,
    COLLAPSING;

    public double spreadMultiplier(PathogenesisConfig.ContaminationConfigs config) {
        return switch (this) {
            case SOURCE_FED -> config.sourceFedSpreadMultiplier;
            case ECOLOGICAL -> config.ecologicalSpreadMultiplier;
            case COLLAPSING -> config.collapsingSpreadMultiplier;
        };
    }

    public boolean diesBack() {
        return this == COLLAPSING;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static OutbreakPhase byId(String id) {
        for (var phase : values()) {
            if (phase.id().equals(id)) {
                return phase;
            }
        }
        return SOURCE_FED;
    }
}
