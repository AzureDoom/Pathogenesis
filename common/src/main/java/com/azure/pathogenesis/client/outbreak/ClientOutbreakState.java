package com.azure.pathogenesis.client.outbreak;

import com.azure.pathogenesis.network.OutbreakStatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ClientOutbreakState {

    private static ResourceKey<Level> dimension;

    private static List<OutbreakStatePayload.Entry> zones = List.of();

    private ClientOutbreakState() {}

    public static void accept(OutbreakStatePayload payload) {
        dimension = payload.dimension();
        zones = List.copyOf(payload.zones());
    }

    public static void clear() {
        dimension = null;
        zones = List.of();
    }

    public static float activityAt(Level level, BlockPos pos) {
        var snapshot = zones;
        if (snapshot.isEmpty() || !level.dimension().equals(dimension)) {
            return 1.0F;
        }
        for (var entry : snapshot) {
            if (entry.contains(pos)) {
                return entry.activity();
            }
        }
        return 1.0F;
    }

    public static boolean roll(Level level, BlockPos pos, float chance, float roll) {
        return roll < chance * activityAt(level, pos);
    }
}
