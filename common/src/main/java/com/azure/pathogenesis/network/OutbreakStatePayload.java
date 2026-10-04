package com.azure.pathogenesis.network;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.contamination.OutbreakPhase;
import com.azure.pathogenesis.contamination.PathogenZone;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record OutbreakStatePayload(
    ResourceKey<Level> dimension,
    List<Entry> zones
) implements CustomPacketPayload {

    public static final Type<OutbreakStatePayload> TYPE = new Type<>(Pathogenesis.id("outbreak_state"));

    public static final StreamCodec<ByteBuf, OutbreakStatePayload> STREAM_CODEC = StreamCodec.composite(
        ResourceKey.streamCodec(Registries.DIMENSION),
        OutbreakStatePayload::dimension,
        Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
        OutbreakStatePayload::zones,
        OutbreakStatePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(
        BlockPos origin,
        int radius,
        OutbreakPhase phase,
        boolean chilled
    ) {

        public static final float CHILLED_ACTIVITY = 0.5F;

        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            Entry::origin,
            ByteBufCodecs.VAR_INT,
            Entry::radius,
            OutbreakPhase.STREAM_CODEC,
            Entry::phase,
            ByteBufCodecs.BOOL,
            Entry::chilled,
            Entry::new
        );

        public static Entry of(PathogenZone zone) {
            return new Entry(zone.origin(), zone.radius(), zone.phase(), zone.isChilled());
        }

        public boolean contains(BlockPos pos) {
            var dx = pos.getX() - origin.getX();
            var dz = pos.getZ() - origin.getZ();
            return dx * dx + dz * dz <= radius * radius
                && Math.abs(pos.getY() - origin.getY()) <= PathogenZone.VERTICAL_REACH;
        }

        public float activity() {
            return phase.activity() * (chilled ? CHILLED_ACTIVITY : 1.0F);
        }
    }
}
