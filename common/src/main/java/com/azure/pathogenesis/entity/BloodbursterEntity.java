package com.azure.pathogenesis.entity;

import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.EmergencyDetector;
import com.azure.azurecortex.runtime.CortexRuntime;
import com.azure.azurecortex.sensing.TargetSensor;
import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.entity.ai.bloodburster.BloodbursterGoal;
import com.azure.pathogenesis.entity.ai.bloodburster.BloodbursterGoalPlanner;
import com.azure.pathogenesis.entity.ai.bloodburster.BloodbursterTree;
import com.azure.pathogenesis.entity.ai.common.CortexGlue;
import com.azure.pathogenesis.entity.ai.common.PathogenBlackboardKeys;
import com.azure.pathogenesis.entity.anim.PathogenAnimationDispatcher;
import com.azure.pathogenesis.infection.NeomorphInfections;
import com.azure.pathogenesis.registry.PathogenEntities;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BloodbursterEntity extends PathfinderMob implements Enemy {

    private static final EntityDataAccessor<Integer> GROWTH = SynchedEntityData.defineId(
        BloodbursterEntity.class,
        EntityDataSerializers.INT
    );

    private final CortexRuntime<BloodbursterEntity, BloodbursterGoal> runtime;

    private final BloodbursterGoalPlanner planner = new BloodbursterGoalPlanner();

    private final List<EmergencyDetector.EmergencyProbe<BloodbursterEntity>> probes;

    private final PathogenAnimationDispatcher animations = new PathogenAnimationDispatcher(this, 0.12D);

    private int ticksSinceFed = 600;

    private int emergeTicks;

    @Nullable
    private UUID originZone;

    public BloodbursterEntity(EntityType<? extends BloodbursterEntity> type, Level level) {
        super(type, level);
        var sensor = new TargetSensor<BloodbursterEntity>(
            TargetSensor.nearestMatching(
                Pathogenesis.getConfig().entityConfigs.bloodbursterConfigs.bloodbursterHostileRange,
                this::isPrey
            ),
            10,
            TargetSensor.lineOfSight()
        );
        this.runtime = new CortexRuntime<>(this, sensor, BloodbursterTree.create());
        this.probes = new ArrayList<>(EmergencyDetector.defaultProbes());
        this.probes.add(agent -> agent.runtime.getBlackboard().get(PathogenBlackboardKeys.THREAT) != null);

        this.runtime.addPeriodicHook(
            "bloodburster_threat_scan",
            10,
            (agent, blackboard) -> blackboard.set(PathogenBlackboardKeys.THREAT, agent.nearestThreat())
        );
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = Pathogenesis.getConfig().entityConfigs.bloodbursterConfigs;
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, config.bloodbursterHealth)
            .add(Attributes.ARMOR, config.bloodbursterArmor)
            .add(Attributes.ARMOR_TOUGHNESS, config.bloodbursterArmorToughness)
            .add(Attributes.KNOCKBACK_RESISTANCE, config.bloodbursterKnockbackRes)
            .add(Attributes.MOVEMENT_SPEED, config.bloodbursterMovementSpeed)
            .add(Attributes.ATTACK_DAMAGE, config.bloodbursterAttackDamage)
            .add(Attributes.FOLLOW_RANGE, config.bloodbursterHostileRange * 1.25D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROWTH, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    public int getGrowth() {
        return entityData.get(GROWTH);
    }

    public float growthProgress() {
        return Mth.clamp(
            getGrowth() / (float) Pathogenesis.getConfig().lifecycleConfigs.bloodbursterGrowthTime,
            0.0F,
            1.0F
        );
    }

    public void setGrowth(int growth) {
        entityData.set(GROWTH, Math.max(0, growth));
        updateScale();
    }

    private void updateScale() {
        var scale = getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(Mth.lerp(growthProgress(), 0.55F, 1.35F));
        }
    }

    public void setOriginZone(@Nullable UUID zone) {
        this.originZone = zone;
    }

    public void markEmerged() {
        this.emergeTicks = 20;
    }

    public PathogenAnimationDispatcher animations() {
        return animations;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        if (emergeTicks > 0) {
            if (emergeTicks-- == 20) {
                animations.playOnce("emerge", 20);
            }
            getNavigation().stop();
            return;
        }
        ticksSinceFed++;
        var growth = getGrowth() + (ticksSinceFed < 600 ? 2 : 1);
        if (growth >= Pathogenesis.getConfig().lifecycleConfigs.bloodbursterGrowthTime) {
            mature((ServerLevel) level());
            return;
        }
        entityData.set(GROWTH, growth);
        if (tickCount % 100 == 0) {
            updateScale();
        }
        if (!isNoAi()) {
            var blackboard = runtime.getBlackboard();
            var reactive = blackboard.get(PathogenBlackboardKeys.THREAT) != null
                || isHungry() && blackboard.get(CommonBlackboardKeys.TARGET) != null;
            CortexGlue.tickPlanner(
                this,
                runtime,
                planner,
                probes,
                goal -> goal == BloodbursterGoal.NONE || goal == BloodbursterGoal.WANDER,
                reactive
            );
            runtime.tick();
        }
        animations.tick();
    }

    public void mature(ServerLevel level) {
        var neophyte = PathogenEntities.NEOPHYTE.get().create(level);
        if (neophyte == null) {
            return;
        }
        neophyte.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        neophyte.setOriginZone(originZone);
        neophyte.setPersistenceRequired();
        if (hasCustomName()) {
            neophyte.setCustomName(getCustomName());
        }
        level.addFreshEntity(neophyte);
        level.sendParticles(NeomorphInfections.BLOOD, getX(), getY() + 0.3D, getZ(), 30, 0.4D, 0.3D, 0.4D, 0.05D);
        level.playSound(
            null,
            blockPosition(),
            PathogenSounds.BLOODBURSTER_MATURE.get(),
            SoundSource.HOSTILE,
            1.2F,
            1.0F
        );
        discard();
    }

    public boolean isHungry() {
        return ticksSinceFed >= 600;
    }

    public boolean isPrey(LivingEntity entity) {
        return entity != this && entity.isAlive() && entity.getType().is(PathogenTags.Entities.BLOODBURSTER_PREY);
    }

    @Nullable
    public LivingEntity nearestThreat() {
        LivingEntity nearest = null;
        var best = 12 * 12;
        for (
            var entity : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(12))
        ) {
            if (
                entity == this || !entity.isAlive() || entity.getType().is(PathogenTags.Entities.ALIEN_ORGANISMS)
                    || isPrey(entity)
            ) {
                continue;
            }
            var threatening = entity instanceof Player player
                ? !player.isCreative() && !player.isSpectator()
                : entity.getBbHeight() * entity.getBbWidth() > getBbHeight() * getBbWidth() * 2.0F
                    && !(entity instanceof Enemy);
            double distance = distanceToSqr(entity);
            if (threatening && distance < best && hasLineOfSight(entity)) {
                best = (int) distance;
                nearest = entity;
            }
        }
        return nearest;
    }

    public boolean isExposed() {
        var pos = blockPosition();
        return !isCovered(level(), pos);
    }

    public static boolean isCovered(Level level, BlockPos pos) {
        var sky = level.canSeeSky(pos);
        var light = level.getBrightness(LightLayer.BLOCK, pos);
        return (!sky || !level.isDay()) && light < 8;
    }

    @Override
    public boolean killedEntity(@NotNull ServerLevel level, @NotNull LivingEntity entity) {
        if (isPrey(entity)) {
            ticksSinceFed = 0;
            setGrowth(getGrowth() + 900);
            heal(2.0F);
        }
        return super.killedEntity(level, entity);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return PathogenSounds.BLOODBURSTER_AMBIENT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return PathogenSounds.BLOODBURSTER_HURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return PathogenSounds.BLOODBURSTER_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Growth", getGrowth());
        tag.putInt("TicksSinceFed", ticksSinceFed);
        if (originZone != null) {
            tag.putUUID("OriginZone", originZone);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setGrowth(tag.getInt("Growth"));
        ticksSinceFed = tag.contains("TicksSinceFed") ? tag.getInt("TicksSinceFed") : 600;
        originZone = tag.hasUUID("OriginZone") ? tag.getUUID("OriginZone") : null;
    }
}
