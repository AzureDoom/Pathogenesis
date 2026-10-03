package com.azure.pathogenesis.infection;

import com.azure.pathogenesis.exposure.PathogenExposureEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PathogenHosts {

    private static final Map<UUID, HostState> HOSTS = new ConcurrentHashMap<>();

    private static final AtomicBoolean CHANGED = new AtomicBoolean();

    private PathogenHosts() {}

    @Nullable
    public static HostState get(LivingEntity entity) {
        return HOSTS.get(entity.getUUID());
    }

    public static HostState getOrCreate(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level) {
            PathogenHostSavedData.get(level);
        }
        return HOSTS.computeIfAbsent(entity.getUUID(), uuid -> {
            CHANGED.set(true);
            var state = new HostState();
            state.lastKnownPos = entity.blockPosition();
            state.dimension = entity.level().dimension();
            state.isPlayer = entity instanceof Player;
            return state;
        });
    }

    public static void remove(LivingEntity entity) {
        if (HOSTS.remove(entity.getUUID()) != null) {
            CHANGED.set(true);
        }
    }

    static Map<UUID, HostState> snapshotForSave() {
        return new HashMap<>(HOSTS);
    }

    static void restore(UUID uuid, HostState state) {
        HOSTS.put(uuid, state);
    }

    static void clearAll() {
        HOSTS.clear();
        CHANGED.set(false);
    }

    public static void tick(ServerLevel level) {
        var data = PathogenHostSavedData.get(level);
        if (HOSTS.isEmpty()) {
            if (CHANGED.getAndSet(false)) {
                data.setDirty();
            }
            return;
        }
        var now = level.getGameTime();
        var it = HOSTS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            var state = entry.getValue();
            var entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity host)) {
                tickAbsent(level, state, now, it);
                continue;
            }
            if (!host.isAlive()) {
                continue;
            }
            if (host instanceof Player player && (player.isCreative() || player.isSpectator())) {
                it.remove();
                CHANGED.set(true);
                continue;
            }
            state.lastKnownPos = host.blockPosition();
            state.dimension = level.dimension();
            state.isPlayer = host instanceof Player;

            var exposure = state.exposure();
            if (exposure != null) {
                exposure.decay(now);
                if (exposure.isEmpty()) {
                    state.clearExposure();
                } else if ((now + host.getId()) % 40 == 0) {
                    PathogenExposureEffects.apply(host, exposure);
                }
            }
            var infection = state.infection();
            if (infection != null) {
                NeomorphInfections.tickInfection(level, host, state, infection);
            }
            if (state.isEmpty()) {
                it.remove();
                CHANGED.set(true);
            }
        }
        data.setDirty();
        CHANGED.set(false);
    }

    private static void tickAbsent(
        ServerLevel level,
        HostState state,
        long now,
        java.util.Iterator<Map.Entry<UUID, HostState>> it
    ) {
        if (state.isPlayer || state.infection() != null) {
            return;
        }
        var dimension = state.dimension != null ? state.dimension : Level.OVERWORLD;
        if (!dimension.equals(level.dimension())) {
            return;
        }
        var exposure = state.exposure();
        if (exposure != null) {
            exposure.decay(now);
            if (exposure.isEmpty()) {
                state.clearExposure();
            }
        }
        if (state.isEmpty()) {
            it.remove();
            CHANGED.set(true);
        }
    }
}
