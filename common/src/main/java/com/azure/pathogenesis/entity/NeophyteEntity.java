package com.azure.pathogenesis.entity;

import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.EmergencyDetector;
import com.azure.azurecortex.runtime.CortexRuntime;
import com.azure.azurecortex.sensing.TargetSensor;
import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.entity.ai.common.CortexGlue;
import com.azure.pathogenesis.entity.ai.common.SoundListener;
import com.azure.pathogenesis.entity.ai.neomorph.NeomorphHearing;
import com.azure.pathogenesis.entity.ai.neophyte.NeophyteGoal;
import com.azure.pathogenesis.entity.ai.neophyte.NeophyteGoalPlanner;
import com.azure.pathogenesis.entity.ai.neophyte.NeophyteTree;
import com.azure.pathogenesis.entity.anim.AnimationDriver;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

public class NeophyteEntity extends Monster implements SoundListener {

    private static final EntityDataAccessor<Integer> GROWTH = SynchedEntityData.defineId(
        NeophyteEntity.class,
        EntityDataSerializers.INT
    );

    private final CortexRuntime<NeophyteEntity, NeophyteGoal> runtime;

    private final NeophyteGoalPlanner goalPlanner = new NeophyteGoalPlanner();

    private final List<EmergencyDetector.EmergencyProbe<NeophyteEntity>> emergencyProbes;

    private final DynamicGameEventListener<NeomorphHearing<NeophyteEntity>> hearing;

    private final AnimationDriver animations = new AnimationDriver("neophyte");

    @Nullable
    private UUID originZone;

    @Nullable
    private BlockPos heardPos;

    private long heardTick;

    private int ticksSinceFed = 600;

    public NeophyteEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        var sensor = new TargetSensor<NeophyteEntity>(
            TargetSensor.nearestMatching(
                Pathogenesis.getConfig().entityConfigs.neophyteConfigs.neophyteHostileRange,
                this::isValidPrey
            ),
            10,
            TargetSensor.lineOfSight()
        );
        this.runtime = new CortexRuntime<>(this, sensor, NeophyteTree.create());
        this.emergencyProbes = new ArrayList<>(EmergencyDetector.defaultProbes());
        this.emergencyProbes.add(agent -> CortexGlue.healthFraction(agent) <= NeophyteGoalPlanner.RETREAT_HEALTH);
        this.hearing = new DynamicGameEventListener<>(new NeomorphHearing<>(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = Pathogenesis.getConfig().entityConfigs.neophyteConfigs;
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, config.neophyteHealth)
            .add(Attributes.MOVEMENT_SPEED, config.neophyteMovementSpeed)
            .add(Attributes.ATTACK_DAMAGE, config.neophyteAttackDamage)
            .add(Attributes.ARMOR, config.neophyteArmor)
            .add(Attributes.ARMOR_TOUGHNESS, config.neophyteArmorToughness)
            .add(Attributes.KNOCKBACK_RESISTANCE, config.neophyteKnockbackRes)
            .add(Attributes.FOLLOW_RANGE, config.neophyteHostileRange * 1.25D)
            .add(Attributes.STEP_HEIGHT, 1.0D)
            .add(Attributes.SAFE_FALL_DISTANCE, 5.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROWTH, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public int getGrowth() {
        return entityData.get(GROWTH);
    }

    public float growthProgress() {
        return Mth.clamp(
            getGrowth() / (float) Pathogenesis.getConfig().lifecycleConfigs.neophyteGrowthTime,
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
            scale.setBaseValue(Mth.lerp(growthProgress(), 0.8F, 1.1F));
        }
    }

    public boolean isValidPrey(LivingEntity entity) {
        if (!entity.isAlive() || entity instanceof SoundListener) {
            return false;
        }
        if (entity.getType().is(PathogenTags.Entities.ALIEN_ORGANISMS)) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return entity.getType().is(PathogenTags.Entities.NEOMORPH_TARGETS);
    }

    @Nullable
    public Entity retreatFrom() {
        var target = runtime.getBlackboard().get(CommonBlackboardKeys.TARGET);
        return target != null ? target : getLastHurtByMob();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        var hurt = super.hurt(source, amount);
        if (
            hurt && !level().isClientSide() && source.getEntity() instanceof LivingEntity attacker && isValidPrey(
                attacker
            )
        ) {
            runtime.getBlackboard().set(CommonBlackboardKeys.TARGET, attacker);
        }
        return hurt;
    }

    @Override
    public void onHeard(BlockPos pos, @Nullable Entity cause) {
        this.heardPos = pos.immutable();
        this.heardTick = level().getGameTime();
    }

    @Override
    @Nullable
    public BlockPos heardPos() {
        return heardPos;
    }

    @Override
    public long heardTick() {
        return heardTick;
    }

    @Override
    public void clearHeard() {
        heardPos = null;
    }

    @Override
    public void updateDynamicGameEventListener(@NotNull BiConsumer<DynamicGameEventListener<?>, ServerLevel> consumer) {
        if (level() instanceof ServerLevel serverLevel) {
            consumer.accept(hearing, serverLevel);
        }
    }

    @SuppressWarnings("unused")
    public void onSlash(LivingEntity target) {
        animations.playOnce(this, "attack", 12);
        playSound(PathogenSounds.NEOMORPH_ATTACK.get(), 0.8F, getVoicePitch());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        ticksSinceFed++;
        var growth = getGrowth() + (ticksSinceFed < 600 ? 2 : 1);
        if (growth >= Pathogenesis.getConfig().lifecycleConfigs.neophyteGrowthTime) {
            mature((ServerLevel) level());
            return;
        }
        entityData.set(GROWTH, growth);
        if (tickCount % 100 == 0) {
            updateScale();
        }
        if (!isNoAi()) {
            CortexGlue.tickPlanner(
                this,
                runtime,
                goalPlanner,
                emergencyProbes,
                goal -> goal instanceof NeophyteGoal g && g.isPassive(),
                runtime.getBlackboard().has(CommonBlackboardKeys.TARGET)
            );
            runtime.tick();
        }
        animations.tickLoop(this, locomotionAnimation());
    }

    private String locomotionAnimation() {
        var speedSqr = getDeltaMovement().horizontalDistanceSqr();
        if (speedSqr < 1.0E-4D) {
            return "idle";
        }
        return speedSqr > 0.025D ? "run" : "walk";
    }

    public void mature(ServerLevel level) {
        var neomorph = PathogenEntities.NEOMORPH.get().create(level);
        if (neomorph == null) {
            return;
        }
        neomorph.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        neomorph.setOriginZone(originZone);
        neomorph.setPersistenceRequired();
        if (hasCustomName()) {
            neomorph.setCustomName(getCustomName());
        }
        level.addFreshEntity(neomorph);
        level.sendParticles(NeomorphInfections.BLOOD, getX(), getY() + 0.8D, getZ(), 30, 0.4D, 0.6D, 0.4D, 0.05D);
        level.playSound(
            null,
            blockPosition(),
            PathogenSounds.NEOMORPH_SCREECH.get(),
            SoundSource.HOSTILE,
            1.5F,
            1.1F
        );
        discard();
    }

    @Override
    public boolean killedEntity(@NotNull ServerLevel level, @NotNull LivingEntity entity) {
        if (entity.getType().is(PathogenTags.Entities.NEOMORPH_TARGETS) || entity instanceof Player) {
            ticksSinceFed = 0;
            setGrowth(getGrowth() + 900);
            heal(4.0F);
        }
        return super.killedEntity(level, entity);
    }

    public void setOriginZone(@Nullable UUID zone) {
        this.originZone = zone;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() + 0.3F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return PathogenSounds.NEOMORPH_AMBIENT.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return PathogenSounds.NEOMORPH_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return PathogenSounds.NEOMORPH_DEATH.get();
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
