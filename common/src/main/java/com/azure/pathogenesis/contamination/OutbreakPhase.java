package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.config.PathogenesisConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

import java.util.Locale;
import java.util.function.IntFunction;

public enum OutbreakPhase {

    SOURCE_FED(1.0F),
    ECOLOGICAL(1.0F),
    COLLAPSING(0.35F);

    private static final IntFunction<OutbreakPhase> BY_ID = ByIdMap.continuous(
        OutbreakPhase::ordinal,
        values(),
        ByIdMap.OutOfBoundsStrategy.ZERO
    );

    public static final StreamCodec<ByteBuf, OutbreakPhase> STREAM_CODEC = ByteBufCodecs.idMapper(
        BY_ID,
        OutbreakPhase::ordinal
    );

    private final float activity;

    OutbreakPhase(float activity) {
        this.activity = activity;
    }

    public double spreadMultiplier(PathogenesisConfig.ContaminationConfigs config) {
        return switch (this) {
            case SOURCE_FED -> config.sourceFedSpreadMultiplier;
            case ECOLOGICAL -> config.ecologicalSpreadMultiplier;
            case COLLAPSING -> config.collapsingSpreadMultiplier;
        };
    }

    public float activity() {
        return activity;
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
