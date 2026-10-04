package com.azure.pathogenesis.contamination;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CarcassSite {

    private final BlockPos pos;

    private CarcassKind kind;

    private boolean bloody;

    private long expiresTick;

    @Nullable
    private UUID zoneId;

    CarcassSite(BlockPos pos, CarcassKind kind, boolean bloody, long expiresTick, @Nullable UUID zoneId) {
        this.pos = pos.immutable();
        this.kind = kind;
        this.bloody = bloody;
        this.expiresTick = expiresTick;
        this.zoneId = zoneId;
    }

    public BlockPos pos() {
        return pos;
    }

    public CarcassKind kind() {
        return kind;
    }

    public boolean isBloody() {
        return bloody;
    }

    public long expiresTick() {
        return expiresTick;
    }

    @Nullable
    public UUID zoneId() {
        return zoneId;
    }

    void merge(CarcassKind kind, boolean bloody, long expiresTick, @Nullable UUID zoneId) {
        this.kind = this.kind.strongest(kind);
        this.bloody |= bloody;
        this.expiresTick = Math.max(this.expiresTick, expiresTick);
        if (this.zoneId == null) {
            this.zoneId = zoneId;
        }
    }

    CompoundTag save() {
        var tag = new CompoundTag();
        tag.putLong("Pos", pos.asLong());
        tag.putString("Kind", kind.id());
        tag.putBoolean("Bloody", bloody);
        tag.putLong("Expires", expiresTick);
        if (zoneId != null) {
            tag.putUUID("Zone", zoneId);
        }
        return tag;
    }

    static CarcassSite load(CompoundTag tag) {
        return new CarcassSite(
            BlockPos.of(tag.getLong("Pos")),
            CarcassKind.byId(tag.getString("Kind")),
            tag.getBoolean("Bloody"),
            tag.getLong("Expires"),
            tag.hasUUID("Zone") ? tag.getUUID("Zone") : null
        );
    }
}
