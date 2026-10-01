package com.azure.pathogenesis.exposure;

import java.util.Locale;

public enum ExposureType {

    /** Broken canister, leak cloud, burst. */
    DIRECT,
    /** Touching Pathogen flora. */
    CONTACT,
    /** Spore inhalation (also starts a Neomorph infection, handled separately). */
    SPORE,
    /** Standing on contaminated terrain. */
    ENVIRONMENTAL;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ExposureType byId(String id) {
        for (ExposureType type : values()) {
            if (type.id().equals(id)) {
                return type;
            }
        }
        return ENVIRONMENTAL;
    }
}
