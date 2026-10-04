package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.config.PathogenesisConfig;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class PathogenClimate {

    private PathogenClimate() {}

    private static PathogenesisConfig.ClimateConfigs config() {
        return Pathogenesis.getConfig().climateConfigs;
    }

    private static boolean enabled() {
        return config().weatherEffectsEnabled;
    }

    public static boolean isWet(Level level, BlockPos pos) {
        return enabled() && (level.isRainingAt(pos) || level.isRainingAt(pos.above()));
    }

    public static boolean isColdAt(Level level, BlockPos pos) {
        if (!enabled()) {
            return false;
        }
        if (level.getBiome(pos).value().coldEnoughToSnow(pos)) {
            return true;
        }
        if (
            isFrost(level.getBlockState(pos)) || isFrost(level.getBlockState(pos.above()))
                || isFrost(level.getBlockState(pos.below()))
        ) {
            return true;
        }
        for (var direction : Direction.Plane.HORIZONTAL) {
            if (isFrost(level.getBlockState(pos.relative(direction)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFrost(BlockState state) {
        return state.is(BlockTags.SNOW) || state.is(BlockTags.ICE)
            || state.is(PathogenBlocks.CONTAMINATED_SNOW.get())
            || state.is(PathogenBlocks.CONTAMINATED_SNOW_BLOCK.get())
            || state.is(PathogenBlocks.CONTAMINATED_ICE.get());
    }

    public static boolean canFreezeContaminatedWater(Level level, BlockPos pos) {
        if (!enabled() || level.getBiome(pos).value().warmEnoughToRain(pos)) {
            return false;
        }
        if (level.getBrightness(LightLayer.BLOCK, pos) >= 10) {
            return false;
        }
        var above = pos.above();
        if (!level.getBlockState(above).isAir() || !level.canSeeSky(above)) {
            return false;
        }
        for (var direction : Direction.Plane.HORIZONTAL) {
            var side = pos.relative(direction);
            if (level.isLoaded(side) && !level.getFluidState(side).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWarmEnoughToThaw(Level level, BlockPos pos) {
        return enabled() && level.getBiome(pos).value().warmEnoughToRain(pos);
    }

    public static float dryness(Level level, BlockPos pos) {
        if (!enabled() || !level.canSeeSky(pos.above()) || isColdAt(level, pos)) {
            return 0.0F;
        }
        var arid = !level.getBiome(pos).value().hasPrecipitation();
        if (level.isRaining()) {
            return arid ? 1.0F : 0.0F;
        }
        return arid ? 1.5F : 1.0F;
    }

    public static double airborneMultiplier(Level level, BlockPos pos) {
        if (!enabled()) {
            return 1.0D;
        }
        if (isWet(level, pos)) {
            return config().rainAirborneMultiplier;
        }
        return 1.0D + (config().dryAirborneMultiplier - 1.0D) * dryness(level, pos);
    }

    public static float sporeActivity(Level level, BlockPos pos) {
        if (!enabled()) {
            return 1.0F;
        }
        if (isColdAt(level, pos)) {
            return (float) config().coldSporeMultiplier;
        }
        return 1.0F + (float) (config().dryAirborneMultiplier - 1.0D) * dryness(level, pos);
    }

    public static float rechargeMultiplier(Level level, BlockPos pos) {
        if (!enabled()) {
            return 1.0F;
        }
        if (isColdAt(level, pos)) {
            return 1.8F;
        }
        return 1.0F / (1.0F + 0.4F * dryness(level, pos));
    }

    public static double floraGrowthMultiplier(Level level, BlockPos pos) {
        return isColdAt(level, pos) ? config().coldFloraMultiplier : 1.0D;
    }

    public static double localSpreadMultiplier(Level level, BlockPos pos) {
        return enabled() && touchesDormantIce(level, pos) ? config().coldSpreadMultiplier : 1.0D;
    }

    private static boolean touchesDormantIce(Level level, BlockPos pos) {
        var cursor = new BlockPos.MutableBlockPos();
        for (var direction : Direction.values()) {
            cursor.setWithOffset(pos, direction);
            if (level.isLoaded(cursor) && level.getBlockState(cursor).is(PathogenTags.Blocks.DORMANT_CONTAMINATION)) {
                return true;
            }
        }
        return false;
    }

    public static double waterSpreadMultiplier(Level level, BlockPos waterPos) {
        return isWet(level, waterPos) ? config().rainWaterSpreadMultiplier : 1.0D;
    }

    public static double zoneActivity(PathogenZone zone, long now) {
        if (!enabled()) {
            return 1.0D;
        }
        if (zone.isChilled()) {
            return config().coldSpreadMultiplier;
        }
        return zone.isThawing(now) ? 2.0D : 1.0D;
    }

    public static double zoneFloraActivity(PathogenZone zone, long now) {
        if (!enabled()) {
            return 1.0D;
        }
        if (zone.isChilled()) {
            return config().coldFloraMultiplier;
        }
        return zone.isThawing(now) ? 1.5D : 1.0D;
    }

    static boolean updateZone(ServerLevel level, PathogenZone zone, long now) {
        if (now < zone.nextClimateTick()) {
            return false;
        }
        zone.setNextClimateTick(now + 100);
        if (!enabled()) {
            if (zone.isChilled()) {
                zone.setChill(0.0F, false, now);
                return true;
            }
            return false;
        }
        var origin = zone.origin();
        var random = level.random;
        var cold = 0;
        var sampled = 0;
        for (var i = 0; i < 6; i++) {
            var angle = random.nextDouble() * Math.PI * 2.0D;
            var distance = Math.sqrt(random.nextDouble()) * zone.radius();
            var x = origin.getX() + Mth.floor(Math.cos(angle) * distance);
            var z = origin.getZ() + Mth.floor(Math.sin(angle) * distance);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            var y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - origin.getY()) > PathogenZone.VERTICAL_REACH) {
                y = origin.getY();
            }
            sampled++;
            if (isColdAt(level, new BlockPos(x, y, z))) {
                cold++;
            }
        }
        if (sampled == 0) {
            return false;
        }
        var chill = zone.chill() + (cold / (float) sampled - zone.chill()) * 0.3F;
        var wasChilled = zone.isChilled();
        var chilled = wasChilled ? chill > 0.3F : chill >= 0.5F;
        if (wasChilled && !chilled && now - zone.chilledSince() >= config().thawMinChilledTicks) {
            zone.setChill(chill, false, now);
            zone.setThawUntil(now + config().thawSurgeTicks);
            onThaw(level, zone);
            return true;
        }
        zone.setChill(chill, chilled, now);
        return true;
    }

    private static void onThaw(ServerLevel level, PathogenZone zone) {
        var c = Vec3.atCenterOf(zone.origin());
        level.sendParticles(PathogenZoneManager.PATHOGEN_DUST, c.x, c.y + 1.0D, c.z, 60, 3.0D, 1.0D, 3.0D, 0.02D);
        level.sendParticles(ParticleTypes.DRIPPING_WATER, c.x, c.y + 1.5D, c.z, 30, 3.0D, 0.5D, 3.0D, 0.0D);
        level.playSound(
            null,
            zone.origin(),
            PathogenSounds.SPORE_RELEASE.get(),
            SoundSource.BLOCKS,
            1.5F,
            0.6F
        );
        Pathogenesis.LOGGER.debug("Pathogen zone {} at {} thawed", zone.id(), zone.origin());
    }
}
