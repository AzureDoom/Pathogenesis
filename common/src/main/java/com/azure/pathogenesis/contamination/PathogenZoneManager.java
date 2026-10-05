package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.exposure.ExposureType;
import com.azure.pathogenesis.exposure.PathogenExposureHelper;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PathogenZoneManager {

    public static final DustParticleOptions PATHOGEN_DUST = new DustParticleOptions(
        new Vector3f(0.05F, 0.05F, 0.07F),
        1.6F
    );

    private static final int ZONE_PROCESS_INTERVAL = 4;

    private static final int CENSUS_INTERVAL = 1200;

    private static final int CENSUS_RETRY = 400;

    private static final int CENSUS_SECTIONS_PER_TICK = 16;

    private static final int MIN_AGE_FOR_ERADICATION = 200;

    private static final double WITNESS_RADIUS = 24.0D;

    private static final Map<ResourceKey<Level>, LevelState> STATES = new HashMap<>();

    private PathogenZoneManager() {}

    public static void tick(ServerLevel level) {
        var config = Pathogenesis.getConfig();
        var data = PathogenSavedData.get(level);
        var now = level.getGameTime();
        CarcassSites.tick(level, data, now);
        OutbreakSync.tick(level, data.zoneList(), now);
        if (!config.contaminationConfigs.pathogenSpreadEnabled || data.zoneList().isEmpty()) {
            return;
        }
        var state = STATES.computeIfAbsent(level.dimension(), key -> new LevelState());
        var zones = data.zoneList();

        var zoneBudget = Math.min(config.contaminationConfigs.zonesProcessedPerTick, zones.size());
        for (var i = 0; i < zoneBudget; i++) {
            state.cursor = (state.cursor + 1) % zones.size();
            var zone = zones.get(state.cursor);
            if (now - zone.lastProcessedTick() < ZONE_PROCESS_INTERVAL || !level.isLoaded(zone.origin())) {
                continue;
            }
            zone.setLastProcessedTick(now);
            if (PathogenClimate.updateZone(level, zone, now)) {
                data.setDirty();
            }
            if (updatePhase(level, zone)) {
                data.setDirty();
            }
            var contamination = config.contaminationConfigs;
            var phaseMultiplier = zone.phase() == OutbreakPhase.SOURCE_FED && zone.isForced()
                ? contamination.forcedSpreadMultiplier
                : zone.phase().spreadMultiplier(contamination);
            var budget = contamination.blockChecksPerZone
                * phaseMultiplier
                * PathogenClimate.zoneActivity(zone, now);
            var checks = rollChecks(budget, level.random.nextDouble());
            if (checks <= 0) {
                continue;
            }
            var before = zone.stage();
            if (PathogenSpreadController.process(level, zone, checks)) {
                data.setDirty();
                if (before != PathogenStage.ESTABLISHED && zone.stage() == PathogenStage.ESTABLISHED) {
                    PathogenTriggers.triggerNearby(
                        level,
                        Vec3.atCenterOf(zone.origin()),
                        zone.radius() + 32.0D,
                        PathogenTriggers.ZONE_ESTABLISHED
                    );
                }
            }
        }

        tickCensus(level, data, state, now);
    }

    private static void tickCensus(ServerLevel level, PathogenSavedData data, LevelState state, long now) {
        if (state.census == null) {
            for (var zone : data.zoneList()) {
                if (zone.nextCensusTick() <= now && level.isLoaded(zone.origin())) {
                    state.census = new ZoneCensus(level, zone);
                    break;
                }
            }
            if (state.census == null) {
                return;
            }
        }
        var census = state.census;
        if (!census.step(level, CENSUS_SECTIONS_PER_TICK)) {
            return;
        }
        state.census = null;
        var zone = data.get(census.zoneId);
        if (zone == null) {
            return;
        }
        if (census.aborted()) {
            zone.setNextCensusTick(now + CENSUS_RETRY);
            return;
        }
        zone.reconcile(census.count(), census.flora(), Pathogenesis.getConfig().contaminationConfigs.pathogenMaxRadius);
        zone.onCensusCompleted(census.startedTick);
        zone.setNextCensusTick(zone.awaitingAssessment() ? now : now + CENSUS_INTERVAL);
        updatePhase(level, zone);
        data.setDirty();

        var eradicated = census.count() == 0
            && !zone.isSourceActive()
            && now - zone.createdTick() >= MIN_AGE_FOR_ERADICATION;
        if (eradicated) {
            eradicate(level, data, zone);
        }
    }

    private static void eradicate(ServerLevel level, PathogenSavedData data, PathogenZone zone) {
        data.remove(zone.id());
        OutbreakSync.pushNearby(level, zone);
        PathogenTriggers.triggerNearby(
            level,
            Vec3.atCenterOf(zone.origin()),
            zone.radius() + 32.0D,
            PathogenTriggers.ZONE_ERADICATED
        );
        if (Pathogenesis.getConfig().debugLogging)
            Pathogenesis.LOGGER.debug("Pathogen zone {} at {} eradicated", zone.id(), zone.origin());
    }

    public static PathogenZone onRupture(
        ServerLevel level,
        BlockPos pos,
        RuptureStrength strength,
        boolean sourceRemains
    ) {
        var data = PathogenSavedData.get(level);
        var zone = findOrCreateZone(level, pos);
        if (sourceRemains) {
            zone.feedSource(strength != RuptureStrength.CRACK);
            updatePhase(level, zone);
        }

        contaminateSphere(level, zone, pos, strength.burstRadius, 0.65F);

        var center = Vec3.atCenterOf(pos);
        PathogenExposureHelper.exposeArea(
            level,
            center,
            strength.exposureRadius,
            strength.exposure,
            ExposureType.DIRECT
        );
        level.sendParticles(PATHOGEN_DUST, center.x, center.y, center.z, 80, 1.2D, 0.8D, 1.2D, 0.02D);
        level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 20, 0.5D, 0.5D, 0.5D, 0.05D);
        level.playSound(null, pos, PathogenSounds.CANISTER_RUPTURE.get(), SoundSource.BLOCKS, 1.4F, 0.9F);
        PathogenTriggers.triggerNearby(level, center, WITNESS_RADIUS, PathogenTriggers.RUPTURE_WITNESSED);
        data.setDirty();
        return zone;
    }

    public static PathogenZone findOrCreateZone(ServerLevel level, BlockPos pos) {
        var zone = findZone(level, pos);
        if (zone == null) {
            zone = new PathogenZone(UUID.randomUUID(), pos, level.getGameTime());
            PathogenSavedData.get(level).add(zone);
        }
        return zone;
    }

    public static void contaminateSphere(ServerLevel level, PathogenZone zone, BlockPos pos, int radius, float chance) {
        if (radius <= 0) {
            return;
        }
        var maxRadius = Pathogenesis.getConfig().contaminationConfigs.pathogenMaxRadius;
        var random = level.random;
        for (
            var target : BlockPos.betweenClosed(
                pos.offset(-radius, -radius, -radius),
                pos.offset(radius, radius, radius)
            )
        ) {
            if (target.distSqr(pos) > radius * radius || !level.isLoaded(target) || random.nextFloat() > chance) {
                continue;
            }
            contaminate(level, zone, target.immutable(), maxRadius);
        }
    }

    public static void contaminate(ServerLevel level, PathogenZone zone, BlockPos pos, int maxRadius) {
        var state = level.getBlockState(pos);
        if (ContaminationPalette.canContaminate(state) && ContaminationPalette.contaminate(level, pos, state)) {
            zone.addContamination(1, maxRadius);
            PathogenSavedData.get(level).setDirty();
        }
    }

    public static void onSourceInactive(ServerLevel level, @Nullable UUID zoneId) {
        var data = PathogenSavedData.get(level);
        var zone = data.get(zoneId);
        if (zone != null && zone.isSourceActive()) {
            var contamination = Pathogenesis.getConfig().contaminationConfigs;
            var grace = zone.isForced() ? contamination.forcedGraceTicks : contamination.establishmentGraceTicks;
            zone.markSourceInactive(level.getGameTime(), grace);
            if (Pathogenesis.getConfig().debugLogging)
                Pathogenesis.LOGGER.debug(
                    "Pathogen zone {} at {} lost its source (forced={}, grace={}, contamination={}, radius={})",
                    zone.id(),
                    zone.origin(),
                    zone.isForced(),
                    grace,
                    zone.contamination(),
                    zone.radius()
                );
            updatePhase(level, zone);
            data.setDirty();
        }
    }

    private static int rollChecks(double budget, double roll) {
        if (budget <= 0.0D) {
            return 0;
        }
        var whole = (int) Math.floor(budget);
        return whole + (roll < budget - whole ? 1 : 0);
    }

    private static boolean updatePhase(ServerLevel level, PathogenZone zone) {
        var wasAwaiting = zone.awaitingAssessment();
        var previous = zone.updatePhase(
            Pathogenesis.getConfig().contaminationConfigs.ecologicalFloraThreshold,
            level.getGameTime()
        );
        if (previous == null) {
            return zone.awaitingAssessment() != wasAwaiting;
        }
        if (Pathogenesis.getConfig().debugLogging)
            Pathogenesis.LOGGER.debug(
                "Pathogen zone {} at {} shifted from {} to {} outbreak (stage={}, contamination={}, radius={}, flora={})",
                zone.id(),
                zone.origin(),
                previous.id(),
                zone.phase().id(),
                zone.stage().id(),
                zone.contamination(),
                zone.radius(),
                zone.flora()
            );
        OutbreakCues.onPhaseChange(level, zone, previous);
        return true;
    }

    @Nullable
    public static PathogenZone findZone(ServerLevel level, BlockPos pos) {
        var maxRadius = Pathogenesis.getConfig().contaminationConfigs.pathogenMaxRadius;
        for (var zone : PathogenSavedData.get(level).zoneList()) {
            if (zone.couldContain(pos, maxRadius)) {
                return zone;
            }
        }
        return null;
    }

    @Nullable
    public static UUID findZoneId(ServerLevel level, BlockPos pos) {
        var zone = findZone(level, pos);
        return zone == null ? null : zone.id();
    }

    public static float floraActivity(ServerLevel level, BlockPos pos) {
        var zone = findZone(level, pos);
        return zone == null ? 1.0F : zone.phase().activity();
    }

    public static List<PathogenZone> zones(ServerLevel level) {
        return PathogenSavedData.get(level).zoneList();
    }

    public static void clearTransientState() {
        STATES.clear();
        OutbreakSync.clear();
    }

    private static final class LevelState {

        int cursor = -1;

        @Nullable
        ZoneCensus census;
    }
}
