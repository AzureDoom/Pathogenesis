package com.azure.pathogenesis.contamination;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum PathogenStage {

    RELEASED,
    CONTAMINATING,
    SPORULATING,
    ESTABLISHED;

    public static final int CONTAMINATING_THRESHOLD = 20;

    public static final int SPORULATING_THRESHOLD = 140;

    public static final int ESTABLISHED_THRESHOLD = 600;

    public boolean allowsFlora() {
        return ordinal() >= CONTAMINATING.ordinal();
    }

    public boolean allowsSpores() {
        return ordinal() >= SPORULATING.ordinal();
    }

    public boolean isEstablished() {
        return this == ESTABLISHED;
    }

    public static PathogenStage forContamination(int contamination, int radius, int maxRadius) {
        if (contamination >= ESTABLISHED_THRESHOLD || radius >= maxRadius * 0.8F) {
            return ESTABLISHED;
        }
        if (contamination >= SPORULATING_THRESHOLD) {
            return SPORULATING;
        }
        if (contamination >= CONTAMINATING_THRESHOLD) {
            return CONTAMINATING;
        }
        return RELEASED;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static PathogenStage byIdOrNull(String id) {
        for (var stage : values()) {
            if (stage.id().equals(id)) {
                return stage;
            }
        }
        return null;
    }

    public static PathogenStage byId(String id) {
        var stage = byIdOrNull(id);
        return stage == null ? RELEASED : stage;
    }
}
