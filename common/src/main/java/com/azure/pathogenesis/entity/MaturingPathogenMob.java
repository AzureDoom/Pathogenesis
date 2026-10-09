package com.azure.pathogenesis.entity;

import com.azure.pathogenesis.contamination.CarcassSites;
import com.azure.pathogenesis.entity.ai.common.PathogenGoal;
import com.azure.pathogenesis.infection.NeomorphInfections;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public abstract class MaturingPathogenMob<SELF extends MaturingPathogenMob<SELF, G>, G extends PathogenGoal> extends PathogenMob<SELF, G> {

    private static final EntityDataAccessor<Integer> GROWTH = SynchedEntityData.defineId(
        MaturingPathogenMob.class,
        EntityDataSerializers.INT
    );

    protected static final int HUNGER_TICKS = 600;

    protected static final int FEED_GROWTH = 900;

    private int ticksSinceFed = HUNGER_TICKS;

    protected MaturingPathogenMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    protected abstract int growthTime();

    protected abstract float minScale();

    protected abstract float maxScale();

    public abstract void mature(ServerLevel level);

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROWTH, 0);
    }

    public int getGrowth() {
        return entityData.get(GROWTH);
    }

    public float growthProgress() {
        return Mth.clamp(getGrowth() / (float) growthTime(), 0.0F, 1.0F);
    }

    public void setGrowth(int growth) {
        entityData.set(GROWTH, Math.max(0, growth));
        updateScale();
    }

    private void updateScale() {
        var scale = getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(Mth.lerp(growthProgress(), minScale(), maxScale()));
        }
    }

    public boolean isHungry() {
        return ticksSinceFed >= HUNGER_TICKS;
    }

    protected boolean tickGrowth(ServerLevel level) {
        ticksSinceFed++;
        var growth = getGrowth() + (ticksSinceFed < HUNGER_TICKS ? 2 : 1);
        if (growth >= growthTime()) {
            mature(level);
            return true;
        }
        entityData.set(GROWTH, growth);
        if (tickCount % 100 == 0) {
            updateScale();
        }
        return false;
    }

    protected void feed(ServerLevel level, LivingEntity prey, float healAmount) {
        ticksSinceFed = 0;
        setGrowth(getGrowth() + FEED_GROWTH);
        heal(healAmount);
        CarcassSites.onFeeding(level, prey, originZone);
    }

    protected void transformInto(
        ServerLevel level,
        EntityType<? extends PathogenMob<?, ?>> nextStage,
        double particleHeight,
        double particleSpreadY,
        SoundEvent sound,
        float volume,
        float pitch
    ) {
        var next = nextStage.create(level);
        if (next == null) {
            return;
        }
        next.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        next.setOriginZone(originZone);
        next.setPersistenceRequired();
        if (hasCustomName()) {
            next.setCustomName(getCustomName());
        }
        level.addFreshEntity(next);
        level.sendParticles(
            NeomorphInfections.BLOOD,
            getX(),
            getY() + particleHeight,
            getZ(),
            30,
            0.4D,
            particleSpreadY,
            0.4D,
            0.05D
        );
        level.playSound(null, blockPosition(), sound, SoundSource.HOSTILE, volume, pitch);
        discard();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Growth", getGrowth());
        tag.putInt("TicksSinceFed", ticksSinceFed);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setGrowth(tag.getInt("Growth"));
        ticksSinceFed = tag.contains("TicksSinceFed") ? tag.getInt("TicksSinceFed") : HUNGER_TICKS;
    }
}
