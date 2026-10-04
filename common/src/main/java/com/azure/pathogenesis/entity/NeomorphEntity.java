package com.azure.pathogenesis.entity;

import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.EmergencyDetector;
import com.azure.azurecortex.runtime.CortexRuntime;
import com.azure.azurecortex.sensing.TargetSensor;
import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.entity.ai.common.*;
import com.azure.pathogenesis.entity.ai.neomorph.NeomorphGoal;
import com.azure.pathogenesis.entity.ai.neomorph.NeomorphGoalPlanner;
import com.azure.pathogenesis.entity.ai.neomorph.NeomorphHearing;
import com.azure.pathogenesis.entity.ai.neomorph.NeomorphTree;
import com.azure.pathogenesis.entity.anim.AnimationDriver;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import com.azure.pathogenesis.registry.PathogenTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

@SuppressWarnings("unused")
public class NeomorphEntity extends Monster implements SoundListener, ScentFollower {

    private final CortexRuntime<NeomorphEntity, NeomorphGoal> runtime;

    private final NeomorphGoalPlanner goalPlanner = new NeomorphGoalPlanner();

    private final List<EmergencyDetector.EmergencyProbe<NeomorphEntity>> emergencyProbes;

    private final DynamicGameEventListener<NeomorphHearing<NeomorphEntity>> hearing;

    private final AnimationDriver animations = new AnimationDriver("neomorph");

    private final ScentTracker scent = new ScentTracker();

    @Nullable
    private UUID originZone;

    @Nullable
    private BlockPos heardPos;

    private long heardTick;

    private boolean stalking;

    @Nullable
    private UUID lastScreechedAt;

    public NeomorphEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        var sensor = new TargetSensor<NeomorphEntity>(
            TargetSensor.nearestMatching(
                Pathogenesis.getConfig().entityConfigs.neomorphConfigs.neomorphHostileRange,
                this::isValidPrey
            ),
            10,
            TargetSensor.lineOfSight()
        );
        this.runtime = new CortexRuntime<>(this, sensor, NeomorphTree.create());
        this.runtime.addPeriodicHook("neomorph_encounter", 20, (agent, blackboard) -> agent.notifyEncounters());
        this.runtime.addPeriodicHook("neomorph_scent", 40, (agent, blackboard) -> {
            if (
                blackboard.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) instanceof NeomorphGoal goal && goal.isPassive()
            ) {
                agent.scent.sniff(agent);
            }
        });
        this.runtime.addPeriodicHook(
            "neomorph_screech",
            10,
            (agent, blackboard) -> agent.maybeScreech(blackboard.get(CommonBlackboardKeys.TARGET))
        );
        this.emergencyProbes = new ArrayList<>(EmergencyDetector.defaultProbes());
        this.emergencyProbes.add(
            agent -> CortexGlue.healthFraction(agent) <= PathogenBlackboardKeys.RETREAT_HEALTH_NEOMORPH
        );
        this.hearing = new DynamicGameEventListener<>(new NeomorphHearing<>(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = Pathogenesis.getConfig().entityConfigs.neomorphConfigs;
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, config.neomorphHealth)
            .add(Attributes.MOVEMENT_SPEED, config.neomorphMovementSpeed)
            .add(Attributes.ATTACK_DAMAGE, config.neomorphAttackDamage)
            .add(Attributes.ATTACK_KNOCKBACK, config.neomorphAttackKnockback)
            .add(Attributes.ARMOR, config.neomorphArmor)
            .add(Attributes.ARMOR_TOUGHNESS, config.neomorphArmorToughness)
            .add(Attributes.KNOCKBACK_RESISTANCE, config.neomorphKnockbackRes)
            .add(Attributes.FOLLOW_RANGE, config.neomorphHostileRange * 1.25D)
            .add(Attributes.STEP_HEIGHT, 1.0D)
            .add(Attributes.SAFE_FALL_DISTANCE, 6.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
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

    public boolean shouldStalk(LivingEntity target) {
        var toMe = position().subtract(target.position()).normalize();
        var facing = target.getViewVector(1.0F).normalize().dot(toMe);
        return facing < 0.3D && getLastHurtByMob() == null;
    }

    public void setStalking(boolean stalking) {
        this.stalking = stalking;
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
    public ScentTracker scent() {
        return scent;
    }

    @Override
    public void updateDynamicGameEventListener(@NotNull BiConsumer<DynamicGameEventListener<?>, ServerLevel> consumer) {
        if (level() instanceof ServerLevel serverLevel) {
            consumer.accept(hearing, serverLevel);
        }
    }

    public void onSlash(LivingEntity target) {
        animations.playOnce(this, "attack", 10);
        playSound(PathogenSounds.NEOMORPH_ATTACK.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
    }

    public void onLeapWindup() {
        animations.playOnce(this, "leap", 20);
    }

    public void onLeapLaunch() {
        playSound(PathogenSounds.NEOMORPH_LEAP.get(), 1.0F, 1.0F);
    }

    private void maybeScreech(@Nullable LivingEntity target) {
        if (target == null || target.getUUID().equals(lastScreechedAt)) {
            return;
        }
        lastScreechedAt = target.getUUID();
        animations.playOnce(this, "screech", 25);
        level().playSound(
            null,
            blockPosition(),
            PathogenSounds.NEOMORPH_SCREECH.get(),
            SoundSource.HOSTILE,
            2.0F,
            0.9F + random.nextFloat() * 0.2F
        );
    }

    private void notifyEncounters() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (ServerPlayer player : serverLevel.players()) {
            if (!player.isSpectator() && player.distanceToSqr(this) <= 20 * 20 && player.hasLineOfSight(this)) {
                PathogenTriggers.trigger(player, PathogenTriggers.NEOMORPH_ENCOUNTERED);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        if (!isNoAi()) {
            CortexGlue.tickPlanner(
                this,
                runtime,
                goalPlanner,
                emergencyProbes,
                goal -> goal instanceof NeomorphGoal g && g.isPassive(),
                runtime.getBlackboard().has(CommonBlackboardKeys.TARGET) || scent.consumeNew()
            );
            runtime.tick();
        }
        animations.tickLoop(this, locomotionAnimation());
    }

    private String locomotionAnimation() {
        var speedSqr = getDeltaMovement().horizontalDistanceSqr();
        if (stalking) {
            return speedSqr < 1.0E-4D ? "stalk_idle" : "stalk";
        }
        if (speedSqr < 1.0E-4D) {
            return "idle";
        }
        return speedSqr > 0.03D ? "run" : "walk";
    }

    public void setOriginZone(@Nullable UUID zone) {
        this.originZone = zone;
    }

    @Nullable
    public UUID originZone() {
        return originZone;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return stalking ? null : PathogenSounds.NEOMORPH_AMBIENT.get();
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
        if (originZone != null) {
            tag.putUUID("OriginZone", originZone);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        originZone = tag.hasUUID("OriginZone") ? tag.getUUID("OriginZone") : null;
    }
}
