package com.azure.pathogenesis.entity;

import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.EmergencyDetector;
import com.azure.azurecortex.runtime.CortexRuntime;
import com.azure.azurecortex.sensing.TargetSensor;
import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.entity.ai.common.CortexGlue;
import com.azure.pathogenesis.entity.ai.popper.PopperGoal;
import com.azure.pathogenesis.entity.ai.popper.PopperGoalPlanner;
import com.azure.pathogenesis.entity.ai.popper.PopperTree;
import com.azure.pathogenesis.entity.anim.AnimationDriver;
import com.azure.pathogenesis.exposure.ExposureType;
import com.azure.pathogenesis.exposure.PathogenExposureHelper;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PopperEntity extends Monster {

    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(
        PopperEntity.class,
        EntityDataSerializers.INT
    );

    private final CortexRuntime<PopperEntity, PopperGoal> runtime;

    private final PopperGoalPlanner planner = new PopperGoalPlanner();

    private final List<EmergencyDetector.EmergencyProbe<PopperEntity>> probes;

    private final AnimationDriver animations = new AnimationDriver("pathogen_popper");

    @Nullable
    private UUID originZone;

    private int idleTicks;

    private int emergeTicks;

    private boolean burst;

    public PopperEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 2;
        var sensor = new TargetSensor<PopperEntity>(
            TargetSensor.nearestMatching(
                Pathogenesis.getConfig().entityConfigs.popperConfigs.popperHostileRange,
                this::isValidPrey
            ),
            10,
            TargetSensor.lineOfSight()
        );
        this.runtime = new CortexRuntime<>(this, sensor, PopperTree.create());
        this.probes = new ArrayList<>(EmergencyDetector.defaultProbes());
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = Pathogenesis.getConfig().entityConfigs.popperConfigs;
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, config.popperHealth)
            .add(Attributes.MOVEMENT_SPEED, config.popperMovementSpeed)
            .add(Attributes.ATTACK_DAMAGE, 0.0D)
            .add(Attributes.FOLLOW_RANGE, config.popperHostileRange * 1.25D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUSE, -1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    public boolean isValidPrey(LivingEntity entity) {
        if (entity == this || !entity.isAlive() || entity.getType().is(PathogenTags.Entities.ALIEN_ORGANISMS)) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return entity.getType().is(PathogenTags.Entities.VALID_HOSTS);
    }

    public int fuse() {
        return entityData.get(FUSE);
    }

    public boolean isFusing() {
        return fuse() >= 0;
    }

    public void startFuse() {
        if (isFusing() || burst) {
            return;
        }
        entityData.set(FUSE, 0);
        animations.playOnce(this, "inflate", Pathogenesis.getConfig().entityConfigs.popperConfigs.popperFuseTicks);
        playSound(PathogenSounds.SPORE_PLANT_RATTLE.get(), 1.0F, 0.6F);
    }

    public void setOriginZone(@Nullable UUID zone) {
        this.originZone = zone;
    }

    public void markEmerged() {
        emergeTicks = 20;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        var level = (ServerLevel) level();
        var config = Pathogenesis.getConfig().entityConfigs.popperConfigs;
        if (isFusing()) {
            var fuse = fuse() + 1;
            entityData.set(FUSE, fuse);
            getNavigation().stop();
            if (fuse % 8 == 0) {
                playSound(PathogenSounds.SPORE_PLANT_RATTLE.get(), 0.8F, 0.6F + fuse / (float) config.popperFuseTicks);
            }
            if (fuse >= config.popperFuseTicks) {
                burst(level, true);
                discard();
            }
            return;
        }
        if (emergeTicks > 0) {
            if (emergeTicks-- == 20) {
                animations.playOnce(this, "emerge", 0);
            }
            getNavigation().stop();
            return;
        }
        if (runtime.getBlackboard().has(CommonBlackboardKeys.TARGET)) {
            idleTicks = 0;
        } else if (++idleTicks >= config.popperLifetime) {
            wilt(level);
            return;
        }
        if (!isNoAi()) {
            CortexGlue.tickPlanner(
                this,
                runtime,
                planner,
                probes,
                goal -> goal instanceof PopperGoal g && g.isPassive(),
                runtime.getBlackboard().has(CommonBlackboardKeys.TARGET)
            );
            runtime.tick();
        }
        animations.tickLoop(this, getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D ? "idle" : "walk");
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level && !burst) {
            if (source.is(DamageTypeTags.IS_FIRE)) {
                fizzle(level);
            } else {
                burst(level, false);
            }
        }
    }

    private void burst(ServerLevel level, boolean full) {
        if (burst) {
            return;
        }
        burst = true;
        var config = Pathogenesis.getConfig().entityConfigs.popperConfigs;
        var radius = full ? config.popperFullContaminationRadius : config.popperPrematureContaminationRadius;
        var size = (float) (full ? config.popperFullCloudSize : config.popperPrematureCloudSize);
        var pos = blockPosition();
        var zone = radius > 0 || size > 0.0F ? PathogenZoneManager.findOrCreateZone(level, pos) : null;
        if (zone != null && radius > 0) {
            PathogenZoneManager.contaminateSphere(level, zone, pos, radius, full ? 0.65F : 0.45F);
        }
        SporeCloudEntity.spawn(level, position().add(0.0D, 0.3D, 0.0D), zone == null ? originZone : zone.id(), size);
        if (full) {
            PathogenExposureHelper.exposeArea(
                level,
                position(),
                config.popperTriggerRange + 1.0D,
                20,
                ExposureType.DIRECT
            );
        }
        level.sendParticles(
            SporeCloudEntity.SPORE_DUST,
            getX(),
            getY() + 0.4D,
            getZ(),
            full ? 60 : 20,
            0.5D,
            0.4D,
            0.5D,
            0.05D
        );
        level.playSound(
            null,
            pos,
            PathogenSounds.SPORE_RELEASE.get(),
            SoundSource.HOSTILE,
            full ? 1.6F : 0.9F,
            full ? 0.7F : 1.1F
        );
    }

    private void fizzle(ServerLevel level) {
        burst = true;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4D, getZ(), 12, 0.3D, 0.3D, 0.3D, 0.02D);
        level.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.8F, 1.4F);
    }

    private void wilt(ServerLevel level) {
        burst = true;
        level.sendParticles(SporeCloudEntity.SPORE_DUST, getX(), getY() + 0.2D, getZ(), 10, 0.3D, 0.1D, 0.3D, 0.01D);
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return PathogenSounds.SPORE_PLANT_RATTLE.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return PathogenSounds.BLOODBURSTER_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return PathogenSounds.SPORE_RELEASE.get();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuse", fuse());
        tag.putInt("IdleTicks", idleTicks);
        if (originZone != null) {
            tag.putUUID("OriginZone", originZone);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FUSE, tag.contains("Fuse") ? tag.getInt("Fuse") : -1);
        idleTicks = tag.getInt("IdleTicks");
        originZone = tag.hasUUID("OriginZone") ? tag.getUUID("OriginZone") : null;
    }
}
