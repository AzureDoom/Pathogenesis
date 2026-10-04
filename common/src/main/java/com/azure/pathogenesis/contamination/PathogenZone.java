package com.azure.pathogenesis.contamination;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PathogenZone {

    public static final int VERTICAL_REACH = 48;

    public static final int EDGE_MARGIN = 8;

    private final UUID id;

    private final BlockPos origin;

    private final long createdTick;

    private int radius = 4;

    private int contamination;

    private PathogenStage stage = PathogenStage.RELEASED;

    private boolean sourceActive;

    private long nextCensusTick;

    private long lastProcessedTick;

    private long nextClimateTick;

    private float chill;

    private boolean chilled;

    private long chilledSince;

    private long thawUntil;

    private int flora = -1;

    private OutbreakPhase phase = OutbreakPhase.SOURCE_FED;

    public PathogenZone(UUID id, BlockPos origin, long createdTick) {
        this.id = id;
        this.origin = origin.immutable();
        this.createdTick = createdTick;
        this.nextCensusTick = createdTick + 600;
    }

    public UUID id() {
        return id;
    }

    public BlockPos origin() {
        return origin;
    }

    public long createdTick() {
        return createdTick;
    }

    public int radius() {
        return radius;
    }

    public int contamination() {
        return contamination;
    }

    public PathogenStage stage() {
        return stage;
    }

    public boolean isSourceActive() {
        return sourceActive;
    }

    public void setSourceActive(boolean sourceActive) {
        this.sourceActive = sourceActive;
    }

    public long nextCensusTick() {
        return nextCensusTick;
    }

    public void setNextCensusTick(long tick) {
        this.nextCensusTick = tick;
    }

    public long lastProcessedTick() {
        return lastProcessedTick;
    }

    public void setLastProcessedTick(long tick) {
        this.lastProcessedTick = tick;
    }

    public long nextClimateTick() {
        return nextClimateTick;
    }

    public void setNextClimateTick(long tick) {
        this.nextClimateTick = tick;
    }

    public float chill() {
        return chill;
    }

    public boolean isChilled() {
        return chilled;
    }

    public long chilledSince() {
        return chilledSince;
    }

    public void setChill(float chill, boolean chilled, long now) {
        if (chilled && !this.chilled) {
            this.chilledSince = now;
        }
        this.chill = chill;
        this.chilled = chilled;
    }

    public boolean isThawing(long now) {
        return now < thawUntil;
    }

    public void setThawUntil(long tick) {
        this.thawUntil = tick;
    }

    public int flora() {
        return flora;
    }

    public boolean isSelfSustaining(int floraThreshold) {
        return stage.isEstablished() && (flora < 0 || flora >= floraThreshold);
    }

    public OutbreakPhase phase() {
        return phase;
    }

    @Nullable
    public OutbreakPhase updatePhase(int floraThreshold) {
        OutbreakPhase next;
        if (sourceActive) {
            next = OutbreakPhase.SOURCE_FED;
        } else if (isSelfSustaining(floraThreshold)) {
            next = OutbreakPhase.ECOLOGICAL;
        } else {
            next = OutbreakPhase.COLLAPSING;
        }
        if (next == phase) {
            return null;
        }
        var previous = phase;
        phase = next;
        return previous;
    }

    public boolean isWithinRadius(BlockPos pos) {
        var dx = pos.getX() - origin.getX();
        var dz = pos.getZ() - origin.getZ();
        return dx * dx + dz * dz <= radius * radius && Math.abs(pos.getY() - origin.getY()) <= VERTICAL_REACH;
    }

    public void addContamination(int amount, int maxRadius) {
        this.contamination = Math.max(0, contamination + amount);
        recompute(maxRadius);
    }

    public void reconcile(int actualCount, int floraCount, int maxRadius) {
        this.contamination = actualCount;
        this.flora = floraCount;
        recompute(maxRadius);
    }

    private void recompute(int maxRadius) {
        this.radius = Mth.clamp(4 + (int) Math.sqrt(contamination * 6.0D), 4, maxRadius);
        if (stage != PathogenStage.DORMANT) {
            this.stage = PathogenStage.forContamination(contamination, radius, maxRadius);
        }
    }

    public boolean couldContain(BlockPos pos, int maxRadius) {
        var dx = pos.getX() - origin.getX();
        var dz = pos.getZ() - origin.getZ();
        return dx * dx + dz * dz <= maxRadius * maxRadius && Math.abs(pos.getY() - origin.getY()) <= VERTICAL_REACH;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putLong("Origin", origin.asLong());
        tag.putLong("Created", createdTick);
        tag.putInt("Radius", radius);
        tag.putInt("Contamination", contamination);
        tag.putString("Stage", stage.id());
        tag.putBoolean("SourceActive", sourceActive);
        tag.putLong("NextCensus", nextCensusTick);
        tag.putFloat("Chill", chill);
        tag.putBoolean("Chilled", chilled);
        tag.putLong("ChilledSince", chilledSince);
        tag.putLong("ThawUntil", thawUntil);
        tag.putInt("Flora", flora);
        tag.putString("Phase", phase.id());
        return tag;
    }

    public static PathogenZone load(CompoundTag tag) {
        var zone = new PathogenZone(
            tag.getUUID("Id"),
            BlockPos.of(tag.getLong("Origin")),
            tag.getLong("Created")
        );
        zone.radius = Math.max(4, tag.getInt("Radius"));
        zone.contamination = tag.getInt("Contamination");
        zone.stage = PathogenStage.byId(tag.getString("Stage"));
        zone.sourceActive = tag.getBoolean("SourceActive");
        zone.nextCensusTick = tag.getLong("NextCensus");
        zone.chill = tag.getFloat("Chill");
        zone.chilled = tag.getBoolean("Chilled");
        zone.chilledSince = tag.getLong("ChilledSince");
        zone.thawUntil = tag.getLong("ThawUntil");
        zone.flora = tag.contains("Flora", Tag.TAG_INT) ? tag.getInt("Flora") : -1;
        zone.phase = tag.contains("Phase", Tag.TAG_STRING)
            ? OutbreakPhase.byId(tag.getString("Phase"))
            : zone.sourceActive ? OutbreakPhase.SOURCE_FED : OutbreakPhase.COLLAPSING;
        return zone;
    }
}
