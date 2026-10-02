package com.azure.pathogenesis.compat;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.api.PathogenApi;
import com.azure.pathogenesis.exposure.PathogenExposureEffects;
import com.azure.pathogenesis.infection.PathogenHosts;
import com.azure.pathogenesis.platform.Services;
import mod.azure.ovomorphosis.api.scanner.InfectionScanners;
import mod.azure.ovomorphosis.api.scanner.ScanReading;
import mod.azure.ovomorphosis.api.scanner.ScanReading.Severity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@SuppressWarnings("unused")
public final class OvomorphosisCompat {

    public static final String OVOMORPHOSIS_ID = "ovomorphosis";

    private static boolean loaded;

    private OvomorphosisCompat() {}

    public static void init() {
        loaded = Services.PLATFORM.isModLoaded(OVOMORPHOSIS_ID);
        if (loaded) {
            Scanner.register();
            Pathogenesis.LOGGER.info(
                "Ovomorphosis detected; alien tags, flame and infection scanner integration active"
            );
        }
    }

    public static boolean isLoaded() {
        return loaded;
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
            if (exposure == null || exposure.exposure() < PathogenExposureEffects.LOW) {
                return null;
            }
            var value = exposure.exposure();
            String band;
            Severity severity;
            if (value >= PathogenExposureEffects.EXTREME) {
                band = "extreme";
                severity = Severity.CRITICAL;
            } else if (value >= PathogenExposureEffects.HIGH) {
                band = "high";
                severity = Severity.WARNING;
            } else if (value >= PathogenExposureEffects.MODERATE) {
                band = "moderate";
                severity = Severity.NOTICE;
            } else {
                band = "low";
                severity = Severity.NOTICE;
            }
            var bandName = Component.translatable("scanner.pathogenesis.exposure." + band);
            var detail = detailed
                ? Component.translatable("scanner.pathogenesis.exposure", bandName, value)
                : Component.translatable("scanner.pathogenesis.exposure_no_value", bandName);
            return new ScanReading(detail, severity);
        }
    }
}
