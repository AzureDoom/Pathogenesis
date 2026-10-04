package com.azure.pathogenesis.item.sampler;

import com.azure.pathogenesis.contamination.PathogenStage;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.IntFunction;

public enum SampleResult implements StringRepresentable {

    CLEAN(0xFFBFE6EE, ChatFormatting.GREEN, 1.6F),
    TRACE(0xFFA8A8AC, ChatFormatting.GRAY, 1.3F),
    ACTIVE(0xFF5C5A60, ChatFormatting.YELLOW, 1.0F),
    SPORULATING(0xFFE2DCC8, ChatFormatting.GOLD, 0.8F),
    ESTABLISHED(0xFF1A181C, ChatFormatting.DARK_RED, 0.6F);

    public static final Codec<SampleResult> CODEC = StringRepresentable.fromEnum(SampleResult::values);

    private static final IntFunction<SampleResult> BY_ID = ByIdMap.continuous(
        SampleResult::ordinal,
        values(),
        ByIdMap.OutOfBoundsStrategy.ZERO
    );

    public static final StreamCodec<ByteBuf, SampleResult> STREAM_CODEC = ByteBufCodecs.idMapper(
        BY_ID,
        SampleResult::ordinal
    );

    public final int vialColor;

    public final ChatFormatting style;

    public final float pitch;

    SampleResult(int vialColor, ChatFormatting style, float pitch) {
        this.vialColor = vialColor;
        this.style = style;
        this.pitch = pitch;
    }

    public static SampleResult forStage(@Nullable PathogenStage stage) {
        if (stage == null) {
            return CLEAN;
        }
        return switch (stage) {
            case RELEASED -> TRACE;
            case CONTAMINATING -> ACTIVE;
            case SPORULATING -> SPORULATING;
            case ESTABLISHED -> ESTABLISHED;
        };
    }

    public SampleResult worst(SampleResult other) {
        return other.ordinal() > ordinal() ? other : this;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public @NotNull String getSerializedName() {
        return id();
    }
}
