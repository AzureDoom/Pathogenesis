package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.block.SporePlantBlock;
import com.azure.pathogenesis.entity.SporeCloudEntity;
import com.azure.pathogenesis.exposure.ExposureTier;
import com.azure.pathogenesis.exposure.ExposureType;
import com.azure.pathogenesis.exposure.PathogenExposureHelper;
import com.azure.pathogenesis.infection.HostState;
import com.azure.pathogenesis.infection.NeomorphInfections;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.UUID;

public final class CarcassSites {

    public static final DustParticleOptions FLIES = new DustParticleOptions(new Vector3f(0.06F, 0.06F, 0.05F), 0.5F);

    private static final BlockParticleOption GORE = new BlockParticleOption(
        ParticleTypes.BLOCK,
        Blocks.REDSTONE_BLOCK.defaultBlockState()
    );

    private static final int AMBIENT_INTERVAL = 10;

    private static final int LINGER_INTERVAL = 40;

    private static final int UNLOADED_GRACE = 6000;

    private static final double MERGE_DISTANCE_SQR = 4.0D;

    private CarcassSites() {}

    public static void onDeath(ServerLevel level, LivingEntity entity, @Nullable HostState state) {
        var config = Pathogenesis.getConfig();
        if (!config.carcassConfigs.carcassSitesEnabled || entity.getType().is(PathogenTags.Entities.PATHOGEN_IMMUNE)) {
            return;
        }
        var pos = entity.blockPosition();
        var kind = classify(level, pos, state);
        if (kind == null) {
            return;
        }
        var baseChance = switch (kind) {
            case FEEDING, MINOR -> 0.5D;
            case INFECTED -> 0.8D;
            case VIRULENT -> 1.0D;
        };
        if (level.random.nextDouble() >= baseChance * config.carcassConfigs.carcassChance) {
            return;
        }
        create(level, pos, kind, kind == CarcassKind.VIRULENT, bodySize(entity), null);

        var exposure = state == null ? null : state.exposure();
        if (
            exposure != null && exposure.tier() == ExposureTier.EXTREME && entity instanceof Animal
                && level.random.nextDouble() < config.faunaConfigs.extremeDeathEventChance
        ) {
            extremeDeathEvent(level, pos);
        }
    }

    public static void onBurst(ServerLevel level, LivingEntity host, @Nullable UUID zoneId) {
        if (Pathogenesis.getConfig().carcassConfigs.carcassSitesEnabled) {
            create(level, host.blockPosition(), CarcassKind.VIRULENT, true, bodySize(host), zoneId);
        }
    }

    @Nullable
    private static CarcassKind classify(ServerLevel level, BlockPos pos, @Nullable HostState state) {
        var tier = state == null || state.exposure() == null ? ExposureTier.NONE : state.exposure().tier();
        var infection = state == null ? null : state.infection();
        if (tier == ExposureTier.EXTREME || infection != null && infection.stage().burstsOnDeath()) {
            return CarcassKind.VIRULENT;
        }
        var zone = PathogenZoneManager.findZone(level, pos);
        var established = zone != null && zone.stage().isEstablished() && zone.isWithinRadius(pos);
        CarcassKind kind = null;
        if (infection != null) {
            kind = CarcassKind.INFECTED;
        } else if (tier == ExposureTier.HIGH) {
            kind = CarcassKind.MINOR;
        }
        if (established) {
            kind = kind == null ? CarcassKind.MINOR : kind.stronger();
        }
        return kind;
    }

    public static void onFeeding(ServerLevel level, LivingEntity victim, @Nullable UUID predatorZone) {
        if (!Pathogenesis.getConfig().carcassConfigs.feedingEvidence) {
            return;
        }
        var y = victim.getY() + victim.getBbHeight() * 0.5D;
        level.sendParticles(NeomorphInfections.BLOOD, victim.getX(), y, victim.getZ(), 25, 0.3D, 0.2D, 0.3D, 0.08D);
        level.sendParticles(GORE, victim.getX(), y, victim.getZ(), 15, 0.25D, 0.2D, 0.25D, 0.15D);
        level.playSound(null, victim.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 1.0F, 0.6F);
        create(level, victim.blockPosition(), CarcassKind.FEEDING, true, bodySize(victim), predatorZone);
    }

    public static void create(
        ServerLevel level,
        BlockPos pos,
        CarcassKind kind,
        boolean bloody,
        float bodySize,
        @Nullable UUID preferredZone
    ) {
        var config = Pathogenesis.getConfig();
        var data = PathogenSavedData.get(level);
        var maxRadius = config.contaminationConfigs.pathogenMaxRadius;
        var duration = (long) Math.max(200, kind.duration * config.carcassConfigs.carcassDurationMultiplier);
        var expires = level.getGameTime() + duration;

        var existing = findSite(data, pos);
        if (existing != null && existing.kind().strongest(kind) == existing.kind()) {
            existing.merge(kind, bloody, expires, null);
            data.setDirty();
            return;
        }

        var zone = data.get(preferredZone);
        if (zone == null || !zone.couldContain(pos, maxRadius)) {
            zone = PathogenZoneManager.findZone(level, pos);
        }
        if (zone == null && kind.seedsZone()) {
            zone = PathogenZoneManager.findOrCreateZone(level, pos);
        }
        var radius = kind.burstRadius + (bodySize > 1.5F ? 1 : 0);
        if (zone != null) {
            PathogenZoneManager.contaminateSphere(level, zone, pos, radius, kind.convertChance);
        }

        var center = Vec3.atBottomCenterOf(pos).add(0.0D, 0.3D, 0.0D);
        if (kind.burstExposure > 0) {
            PathogenExposureHelper.exposeArea(
                level,
                center,
                kind.burstExposureRadius,
                kind.burstExposure,
                ExposureType.DIRECT
            );
        }
        var dust = 12 + kind.ordinal() * 14;
        level.sendParticles(
            PathogenZoneManager.PATHOGEN_DUST,
            center.x,
            center.y,
            center.z,
            dust,
            0.6D,
            0.3D,
            0.6D,
            0.02D
        );
        if (bloody) {
            level.sendParticles(NeomorphInfections.BLOOD, center.x, center.y, center.z, 20, 0.4D, 0.1D, 0.4D, 0.02D);
        }
        if (kind != CarcassKind.FEEDING) {
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 0.8F, 0.5F);
        }

        UUID zoneId = zone == null ? null : zone.id();
        if (existing != null) {
            existing.merge(kind, bloody, expires, zoneId);
            data.setDirty();
            return;
        }
        var sites = data.carcasses();
        var cap = config.carcassConfigs.maxCarcassSites;
        if (cap <= 0) {
            return;
        }
        while (sites.size() >= cap) {
            sites.stream().min(Comparator.comparingLong(CarcassSite::expiresTick)).ifPresent(sites::remove);
        }
        sites.add(new CarcassSite(pos, kind, bloody, expires, zoneId));
        data.setDirty();
    }

    @Nullable
    private static CarcassSite findSite(PathogenSavedData data, BlockPos pos) {
        for (var site : data.carcasses()) {
            if (site.pos().distSqr(pos) <= MERGE_DISTANCE_SQR) {
                return site;
            }
        }
        return null;
    }

    private static void extremeDeathEvent(ServerLevel level, BlockPos pos) {
        SporeCloudEntity.spawn(
            level,
            Vec3.atBottomCenterOf(pos).add(0.0D, 0.4D, 0.0D),
            PathogenZoneManager.findZoneId(level, pos)
        );
        SporePlantBlock.disturbNearby(level, pos, 2);
        level.playSound(null, pos, PathogenSounds.SPORE_RELEASE.get(), SoundSource.NEUTRAL, 1.0F, 0.7F);
    }

    static void tick(ServerLevel level, PathogenSavedData data, long now) {
        var sites = data.carcasses();
        if (sites.isEmpty()) {
            return;
        }
        var enabled = Pathogenesis.getConfig().carcassConfigs.carcassSitesEnabled;
        var changed = false;
        var it = sites.iterator();
        while (it.hasNext()) {
            var site = it.next();
            var pos = site.pos();
            if (!enabled) {
                it.remove();
                changed = true;
                continue;
            }
            if (!level.isLoaded(pos)) {
                if (now > site.expiresTick() + UNLOADED_GRACE) {
                    it.remove();
                    changed = true;
                }
                continue;
            }
            if (now >= site.expiresTick()) {
                decompose(level, data, site);
                it.remove();
                changed = true;
                continue;
            }
            var phase = now + (pos.asLong() & 0xFF);
            if (phase % LINGER_INTERVAL == 0 && isCleansed(level, pos)) {
                var c = Vec3.atCenterOf(pos);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 6, 0.3D, 0.2D, 0.3D, 0.01D);
                it.remove();
                changed = true;
                continue;
            }
            if (phase % AMBIENT_INTERVAL == 0) {
                ambient(level, site, phase);
            }
        }
        if (changed) {
            data.setDirty();
        }
    }

    private static boolean isCleansed(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof BaseFireBlock
            || level.getBlockState(pos.below()).is(PathogenBlocks.STERILIZED_SOIL.get());
    }

    private static void ambient(ServerLevel level, CarcassSite site, long phase) {
        var random = level.random;
        var c = Vec3.atBottomCenterOf(site.pos());
        level.sendParticles(FLIES, c.x, c.y + 0.5D, c.z, 2 + random.nextInt(3), 0.45D, 0.35D, 0.45D, 0.03D);
        if (site.isBloody() && random.nextInt(3) == 0) {
            level.sendParticles(NeomorphInfections.BLOOD, c.x, c.y + 0.05D, c.z, 2, 0.35D, 0.02D, 0.35D, 0.0D);
        }
        if (site.kind() != CarcassKind.FEEDING && random.nextInt(3) == 0) {
            level.sendParticles(PathogenZoneManager.PATHOGEN_DUST, c.x, c.y + 0.2D, c.z, 2, 0.4D, 0.1D, 0.4D, 0.01D);
        }
        if (random.nextInt(14) == 0) {
            level.playSound(null, site.pos(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.35F, 0.6F);
        }
        if (site.kind().lingerExposure > 0 && phase % LINGER_INTERVAL == 0) {
            PathogenExposureHelper.exposeArea(
                level,
                c.add(0.0D, 0.5D, 0.0D),
                1.75D,
                site.kind().lingerExposure,
                ExposureType.ENVIRONMENTAL
            );
        }
    }

    private static void decompose(ServerLevel level, PathogenSavedData data, CarcassSite site) {
        var random = level.random;
        var kind = site.kind();
        if (random.nextFloat() >= kind.growthChance) {
            return;
        }
        var pos = site.pos();
        var ground = pos.below();
        var current = level.getBlockState(pos);
        if (
            !(current.isAir() || current.canBeReplaced())
                || !level.getBlockState(ground).is(PathogenTags.Blocks.CONTAMINATED_SOIL)
        ) {
            return;
        }
        var spore = PathogenBlocks.SPORE_PLANT.get().defaultBlockState();
        var growth = PathogenBlocks.PATHOGEN_GROWTH.get().defaultBlockState();
        var placed = random.nextFloat() < kind.sporeChance && spore.canSurvive(level, pos) ? spore : growth;
        if (!placed.canSurvive(level, pos) || !level.setBlock(pos, placed, Block.UPDATE_ALL)) {
            return;
        }
        var zone = data.get(site.zoneId());
        if (zone != null) {
            zone.addContamination(1, Pathogenesis.getConfig().contaminationConfigs.pathogenMaxRadius);
        }
    }

    public static void clearNear(ServerLevel level, BlockPos center, int radius) {
        var data = PathogenSavedData.get(level);
        var sites = data.carcasses();
        if (sites.isEmpty()) {
            return;
        }
        var rSqr = (double) (radius + 1) * (radius + 1);
        if (sites.removeIf(site -> site.pos().distSqr(center) <= rSqr)) {
            data.setDirty();
        }
    }

    private static float bodySize(LivingEntity entity) {
        return entity.getBbWidth() * entity.getBbHeight();
    }
}
