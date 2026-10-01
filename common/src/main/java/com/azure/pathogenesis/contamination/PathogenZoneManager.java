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
        if (!config.contaminationConfigs.pathogenSpreadEnabled) {
            return;
        }
        var data = PathogenSavedData.get(level);
        if (data.isEmpty()) {
            return;
        }
        var state = STATES.computeIfAbsent(level.dimension(), key -> new LevelState());
        var now = level.getGameTime();
        var zones = data.zoneList();

        var budget = Math.min(config.contaminationConfigs.zonesProcessedPerTick, zones.size());
        for (var i = 0; i < budget; i++) {
            state.cursor = (state.cursor + 1) % zones.size();
            var zone = zones.get(state.cursor);
            if (now - zone.lastProcessedTick() < ZONE_PROCESS_INTERVAL || !level.isLoaded(zone.origin())) {
                continue;
            }
            zone.setLastProcessedTick(now);
            var checks = zone.isSourceActive()
                ? config.contaminationConfigs.blockChecksPerZone * 2
                : config.contaminationConfigs.blockChecksPerZone;
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
        zone.reconcile(census.count(), Pathogenesis.getConfig().contaminationConfigs.pathogenMaxRadius);
        zone.setNextCensusTick(now + CENSUS_INTERVAL);
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
        PathogenTriggers.triggerNearby(
            level,
            Vec3.atCenterOf(zone.origin()),
            zone.radius() + 32.0D,
            PathogenTriggers.ZONE_ERADICATED
        );
        Pathogenesis.LOGGER.debug("Pathogen zone {} at {} eradicated", zone.id(), zone.origin());
    }

    public static PathogenZone onRupture(
        ServerLevel level,
        BlockPos pos,
        RuptureStrength strength,
        boolean sourceRemains
    ) {
        var config = Pathogenesis.getConfig();
        var data = PathogenSavedData.get(level);
        var zone = findZone(level, pos);
        if (zone == null) {
            zone = new PathogenZone(UUID.randomUUID(), pos, level.getGameTime());
            data.add(zone);
        }
        if (sourceRemains) {
            zone.setSourceActive(true);
        }

        var random = level.random;
        var r = strength.burstRadius;
        for (var target : BlockPos.betweenClosed(pos.offset(-r, -r, -r), pos.offset(r, r, r))) {
            if (target.distSqr(pos) > r * r || !level.isLoaded(target) || random.nextFloat() > 0.65F) {
                continue;
            }
            var state = level.getBlockState(target);
            if (
                ContaminationPalette.canContaminate(state) && ContaminationPalette.contaminate(
                    level,
                    target.immutable(),
                    state
                )
            ) {
                zone.addContamination(1, config.contaminationConfigs.pathogenMaxRadius);
            }
        }

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

    public static void onSourceInactive(ServerLevel level, @Nullable UUID zoneId) {
        var data = PathogenSavedData.get(level);
        var zone = data.get(zoneId);
        if (zone != null && zone.isSourceActive()) {
            zone.setSourceActive(false);
            data.setDirty();
        }
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

    public static List<PathogenZone> zones(ServerLevel level) {
        return PathogenSavedData.get(level).zoneList();
    }

    public static void clearTransientState() {
        STATES.clear();
    }

    private static final class LevelState {

        int cursor = -1;

        @Nullable
        ZoneCensus census;
    }
}
