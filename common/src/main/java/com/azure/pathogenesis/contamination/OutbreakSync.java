package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.network.OutbreakStatePayload;
import com.azure.pathogenesis.platform.Services;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OutbreakSync {

    private static final Map<ResourceKey<Level>, Set<UUID>> SYNCED = new HashMap<>();

    private OutbreakSync() {}

    static void tick(ServerLevel level, List<PathogenZone> zones, long now) {
        if (now % 40 != 0) {
            return;
        }
        var synced = SYNCED.get(level.dimension());
        if (zones.isEmpty() && (synced == null || synced.isEmpty())) {
            return;
        }
        for (var player : level.players()) {
            send(level, player, zones);
        }
    }

    static void pushNearby(ServerLevel level, PathogenZone zone) {
        var zones = PathogenSavedData.get(level).zoneList();
        var reach = zone.radius() + 48.0D;
        for (var player : level.players()) {
            if (horizontalDistanceSqr(player, zone) <= reach * reach) {
                send(level, player, zones);
            }
        }
    }

    private static void send(ServerLevel level, ServerPlayer player, List<PathogenZone> zones) {
        var synced = SYNCED.computeIfAbsent(level.dimension(), key -> new HashSet<>());
        var entries = new ArrayList<OutbreakStatePayload.Entry>();
        for (var zone : zones) {
            var reach = zone.radius() + 48.0D;
            if (horizontalDistanceSqr(player, zone) <= reach * reach) {
                entries.add(OutbreakStatePayload.Entry.of(zone));
            }
        }
        if (entries.isEmpty()) {
            if (!synced.remove(player.getUUID())) {
                return;
            }
        } else {
            synced.add(player.getUUID());
        }
        Services.PLATFORM.sendToPlayer(player, new OutbreakStatePayload(level.dimension(), entries));
    }

    private static double horizontalDistanceSqr(ServerPlayer player, PathogenZone zone) {
        var dx = player.getX() - (zone.origin().getX() + 0.5D);
        var dz = player.getZ() - (zone.origin().getZ() + 0.5D);
        return dx * dx + dz * dz;
    }

    static void clear() {
        SYNCED.clear();
    }
}
