package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.Pathogenesis;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PathogenZone {

    public static final int VERTICAL_REACH = 48;

    public static final int EDGE_MARGIN = 8;

    private static final long NO_ASSESSMENT = -1L;

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

    private boolean sourceWithdrawn;

    private long graceUntil;

    private long assessmentRequested = NO_ASSESSMENT;

    private long assessmentDeadline;

    private boolean assessed;

    private boolean forced;

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
        if (sourceActive) {
            clearSourcelessTransition();
        }
    }

    public boolean isForced() {
        return forced;
    }

    public void feedSource(boolean forcedSource) {
        this.forced = forcedSource || (forced && sourceActive);
        setSourceActive(true);
    }

    public void markSourceInactive(long now, int graceTicks) {
        this.sourceActive = false;
        clearSourcelessTransition();
        this.sourceWithdrawn = true;
        this.graceUntil = now + Math.max(0, graceTicks);
    }

    private void clearSourcelessTransition() {
        this.sourceWithdrawn = false;
        this.graceUntil = 0L;
        this.assessmentRequested = NO_ASSESSMENT;
        this.assessmentDeadline = 0L;
        this.assessed = false;
    }

    public boolean awaitingAssessment() {
        return assessmentRequested != NO_ASSESSMENT && !assessed;
    }

    public void onCensusCompleted(long censusStartedTick) {
        if (awaitingAssessment() && censusStartedTick >= assessmentRequested) {
            assessed = true;
        }
    }

    private void requestAssessment(long now) {
        this.assessmentRequested = now;
        this.assessmentDeadline = now + 600;
        this.assessed = false;
        if (nextCensusTick > now) {
            this.nextCensusTick = now;
        }
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
        return stage.allowsSpores() && (flora < 0 || flora >= floraThreshold);
    }

    public OutbreakPhase phase() {
        return phase;
    }

    @Nullable
    public OutbreakPhase updatePhase(int floraThreshold, long now) {
        OutbreakPhase next;
        if (sourceActive) {
            next = OutbreakPhase.SOURCE_FED;
        } else if (isSelfSustaining(floraThreshold)) {
            next = OutbreakPhase.ECOLOGICAL;
        } else if (phase == OutbreakPhase.SOURCE_FED && sourceWithdrawn) {
            next = resolveSourcelessTransition(now);
        } else {
            next = OutbreakPhase.COLLAPSING;
        }
        if (next == phase) {
            return null;
        }
        var previous = phase;
        phase = next;
        if (next != OutbreakPhase.SOURCE_FED) {
            clearSourcelessTransition();
            forced = false;
        }
        return previous;
    }

    private OutbreakPhase resolveSourcelessTransition(long now) {
        if (now < graceUntil) {
            return OutbreakPhase.SOURCE_FED;
        }
        if (assessmentRequested == NO_ASSESSMENT) {
            requestAssessment(now);
            return OutbreakPhase.SOURCE_FED;
        }
        if (!assessed && now < assessmentDeadline) {
            return OutbreakPhase.SOURCE_FED;
        }
        return OutbreakPhase.COLLAPSING;
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
        this.radius = Mth.clamp(4 + (int) (1.5D * Math.sqrt(contamination / Math.PI)), 4, maxRadius);
        this.stage = PathogenStage.forContamination(contamination, radius, maxRadius);
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
        tag.putBoolean("SourceWithdrawn", sourceWithdrawn);
        tag.putLong("GraceUntil", graceUntil);
        tag.putLong("AssessmentRequested", assessmentRequested);
        tag.putLong("AssessmentDeadline", assessmentDeadline);
        tag.putBoolean("Assessed", assessed);
        tag.putBoolean("Forced", forced);
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
        var stage = PathogenStage.byIdOrNull(tag.getString("Stage"));
        zone.stage = stage != null
            ? stage
            : PathogenStage.forContamination(
                zone.contamination,
                zone.radius,
                Pathogenesis.getConfig().contaminationConfigs.pathogenMaxRadius
            );
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
        zone.sourceWithdrawn = tag.getBoolean("SourceWithdrawn");
        zone.graceUntil = tag.getLong("GraceUntil");
        zone.assessmentRequested = tag.contains("AssessmentRequested", Tag.TAG_LONG)
            ? tag.getLong("AssessmentRequested")
            : NO_ASSESSMENT;
        zone.assessmentDeadline = tag.getLong("AssessmentDeadline");
        zone.assessed = tag.getBoolean("Assessed");
        zone.forced = tag.getBoolean("Forced");
        return zone;
    }
}
