package com.azure.pathogenesis.entity;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.infection.NeomorphInfections;
import com.azure.pathogenesis.registry.PathogenEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

public class SporeCloudEntity extends Entity {

    public static final DustParticleOptions SPORE_DUST = new DustParticleOptions(
        new Vector3f(0.82F, 0.80F, 0.74F),
        1.2F
    );

    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(
        SporeCloudEntity.class,
        EntityDataSerializers.FLOAT
    );

    private int lifetime;

    @Nullable
    private UUID zoneId;

    public SporeCloudEntity(EntityType<? extends SporeCloudEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.lifetime = Pathogenesis.getConfig().sporeConfigs.sporeCloudDuration;
    }

    public static void spawn(ServerLevel level, Vec3 pos, @Nullable UUID zoneId) {
        var cloud = new SporeCloudEntity(PathogenEntities.SPORE_CLOUD.get(), level);
        cloud.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        cloud.zoneId = zoneId;
        cloud.setRadius(0.5F);
        level.addFreshEntity(cloud);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 0.5F);
    }

    public float getRadius() {
        return entityData.get(RADIUS);
    }

    public void setRadius(float radius) {
        entityData.set(RADIUS, radius);
    }

    @SuppressWarnings("unused")
    @Nullable
    public UUID zoneId() {
        return zoneId;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            spawnClientParticles();
            return;
        }
        var level = (ServerLevel) level();
        var maxRadius = (float) Pathogenesis.getConfig().sporeConfigs.sporeInfectionRadius;
        if (tickCount <= 20) {
            setRadius(Mth.lerp(tickCount / 20F, 0.5F, maxRadius));
        }
        setPos(
            getX() + Mth.sin(tickCount * 0.05F) * 0.005D,
            getY() - 0.002D,
            getZ() + Mth.cos(tickCount * 0.05F) * 0.005D
        );

        if (tickCount >= lifetime || isInWaterOrRain() || isDispersedByFire(level)) {
            if (tickCount < lifetime) {
                level.sendParticles(
                    ParticleTypes.SMOKE,
                    getX(),
                    getY() + 0.5D,
                    getZ(),
                    12,
                    getRadius() * 0.4D,
                    0.3D,
                    getRadius() * 0.4D,
                    0.01D
                );
                level.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.8F);
            }
            discard();
            return;
        }
        if (tickCount % 4 == 0) {
            infectOverlapping(level);
        }
    }

    private boolean isDispersedByFire(ServerLevel level) {
        if (tickCount % 4 != 0) {
            return false;
        }
        var box = cloudBox();
        for (
            var pos : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY - 1, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ)
            )
        ) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof BaseFireBlock || state.getFluidState().is(FluidTags.LAVA)) {
                return true;
            }
        }
        return !level.getEntitiesOfClass(Entity.class, box, Entity::isOnFire).isEmpty();
    }

    private void infectOverlapping(ServerLevel level) {
        var radius = getRadius();
        var radiusSqr = radius * radius;
        var center = position().add(0.0D, 0.5D, 0.0D);
        var cloudPos = blockPosition();
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, cloudBox())) {
            var body = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            var dx = body.x - center.x;
            var dz = body.z - center.z;
            var dy = (body.y - center.y) * 0.6D;
            if (dx * dx + dy * dy + dz * dz <= radiusSqr) {
                NeomorphInfections.exposeToSpores(entity, zoneId, cloudPos);
            }
        }
    }

    private AABB cloudBox() {
        var r = getRadius();
        return new AABB(getX() - r, getY() - 0.5D, getZ() - r, getX() + r, getY() + 1.5D + r * 0.5D, getZ() + r);
    }

    private void spawnClientParticles() {
        var r = getRadius();
        var count = 2 + (int) (r * 3.0F);
        for (var i = 0; i < count; i++) {
            var angle = random.nextDouble() * Math.PI * 2.0D;
            var dist = Math.sqrt(random.nextDouble()) * r;
            level().addParticle(
                SPORE_DUST,
                getX() + Math.cos(angle) * dist,
                getY() + random.nextDouble() * (1.0D + r * 0.4D),
                getZ() + Math.sin(angle) * dist,
                0.0D,
                0.005D,
                0.0D
            );
        }
        if (random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.WHITE_ASH, getX(), getY() + 0.5D, getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("Age");
        if (tag.contains("Lifetime")) {
            lifetime = tag.getInt("Lifetime");
        }
        setRadius(tag.contains("Radius") ? tag.getFloat("Radius") : 0.5F);
        zoneId = tag.hasUUID("Zone") ? tag.getUUID("Zone") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", tickCount);
        tag.putInt("Lifetime", lifetime);
        tag.putFloat("Radius", getRadius());
        if (zoneId != null) {
            tag.putUUID("Zone", zoneId);
        }
    }
}
