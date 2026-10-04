package com.azure.pathogenesis.registry;

import com.azure.pathogenesis.advancement.PathogenEventTrigger;
import com.azure.pathogenesis.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public final class PathogenTriggers {

    public static final Supplier<PathogenEventTrigger> EVENT = Services.REGISTRY.register(
        Registries.TRIGGER_TYPE,
        "event",
        PathogenEventTrigger::new
    );

    public static final String RUPTURE_WITNESSED = "rupture_witnessed";

    public static final String SPORE_EXPOSED = "spore_exposed";

    public static final String INFECTED = "infected";

    public static final String BLOODBURSTER_WITNESSED = "bloodburster_witnessed";

    public static final String NEOMORPH_ENCOUNTERED = "neomorph_encountered";

    public static final String STERILIZED = "sterilized";

    public static final String ZONE_ESTABLISHED = "zone_established";

    public static final String ZONE_ECOLOGICAL = "zone_ecological";

    public static final String ZONE_ERADICATED = "zone_eradicated";

    public static final String FIELD_SAMPLE = "field_sample";

    private PathogenTriggers() {}

    public static void trigger(ServerPlayer player, String event) {
        EVENT.get().trigger(player, event);
    }

    public static void triggerNearby(ServerLevel level, Vec3 pos, double radius, String event) {
        var radiusSqr = radius * radius;
        for (var player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(pos) <= radiusSqr) {
                trigger(player, event);
            }
        }
    }

    public static void init() {}
}
