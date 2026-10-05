package com.azure.pathogenesis.compat;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.api.PathogenApi;
import com.azure.pathogenesis.infection.PathogenHosts;
import com.azure.pathogenesis.platform.Services;
import mod.azure.ovomorphosis.api.scanner.InfectionScanners;
import mod.azure.ovomorphosis.api.scanner.ScanReading;
import mod.azure.ovomorphosis.api.scanner.ScanReading.Severity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@SuppressWarnings("unused")
public final class OvomorphosisCompat {

    public static final String OVOMORPHOSIS_ID = "ovomorphosis";

    private static final ResourceLocation ACID = ResourceLocation.fromNamespaceAndPath(OVOMORPHOSIS_ID, "acid");

    private static boolean loaded;

    private static boolean acidMissingLogged;

    private OvomorphosisCompat() {}

    public static void init() {
        loaded = Services.PLATFORM.isModLoaded(OVOMORPHOSIS_ID);
        if (loaded) {
            Scanner.register();
            if (Pathogenesis.getConfig().debugLogging)
                Pathogenesis.LOGGER.info(
                    "Ovomorphosis detected; alien tags, flame and infection scanner integration active"
                );
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static boolean spawnAcid(ServerLevel level, Vec3 pos) {
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(ACID);
        if (type.isEmpty()) {
            if (!acidMissingLogged) {
                acidMissingLogged = true;
                Pathogenesis.LOGGER.warn("Ovomorphosis acid entity {} is not registered; acid blood disabled", ACID);
            }
            return false;
        }
        var acid = type.get().create(level);
        if (acid == null) {
            return false;
        }
        acid.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F);
        return level.addFreshEntity(acid);
    }

    public static void onFlameImpact(ServerLevel level, BlockPos pos) {
        PathogenApi.sterilizeArea(level, pos, 1);
    }

    private static final class Scanner {

        private Scanner() {}

        static void register() {
            InfectionScanners.register((target, scanner, detailed) -> read(target, detailed));
        }

        @Nullable
        private static ScanReading read(LivingEntity target, boolean detailed) {
            var host = PathogenHosts.get(target);
            if (host == null) {
                return null;
            }

            var infection = host.infection();
            if (infection != null) {
                var stage = infection.stage();
                var stageName = Component.translatable(
                    "scanner.pathogenesis.stage." + stage.name().toLowerCase(Locale.ROOT)
                );
                var detail = detailed
                    ? Component.translatable(
                        "scanner.pathogenesis.infected",
                        stageName,
                        (int) (infection.progress() * 100)
                    )
                    : Component.translatable("scanner.pathogenesis.infected_no_time", stageName);
                var severity = switch (stage) {
                    case EXPOSED, INCUBATING -> Severity.NOTICE;
                    case SYMPTOMATIC -> Severity.WARNING;
                    case TERMINAL -> Severity.CRITICAL;
                };
                return new ScanReading(detail, severity);
            }

            var exposure = host.exposure();
            if (exposure == null) {
                return null;
            }
            var tier = exposure.tier();
            var severity = switch (tier) {
                case NONE -> null;
                case LOW, MODERATE -> Severity.NOTICE;
                case HIGH -> Severity.WARNING;
                case EXTREME -> Severity.CRITICAL;
            };
            if (severity == null) {
                return null;
            }
            var bandName = Component.translatable("scanner.pathogenesis.exposure." + tier.id());
            var detail = detailed
                ? Component.translatable("scanner.pathogenesis.exposure", bandName, exposure.exposure())
                : Component.translatable("scanner.pathogenesis.exposure_no_value", bandName);
            return new ScanReading(detail, severity);
        }
    }
}
