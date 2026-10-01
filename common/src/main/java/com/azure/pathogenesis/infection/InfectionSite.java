package com.azure.pathogenesis.infection;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record InfectionSite(
    BlockPos pos,
    @Nullable UUID zoneId
) {

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putLong("Pos", pos.asLong());
        if (zoneId != null) {
            tag.putUUID("Zone", zoneId);
        }
        return tag;
    }

    public static InfectionSite load(CompoundTag tag) {
        return new InfectionSite(BlockPos.of(tag.getLong("Pos")), tag.hasUUID("Zone") ? tag.getUUID("Zone") : null);
    }
}
