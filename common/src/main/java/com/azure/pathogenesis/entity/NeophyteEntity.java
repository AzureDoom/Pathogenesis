package com.azure.pathogenesis.entity;

import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.GoalPlanner;
import com.azure.azurecortex.sensing.TargetSensor;
import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.entity.ai.common.*;
import com.azure.pathogenesis.entity.ai.neophyte.NeophyteGoal;
import com.azure.pathogenesis.entity.ai.neophyte.NeophyteGoalPlanner;
import com.azure.pathogenesis.entity.ai.neophyte.NeophyteTree;
import com.azure.pathogenesis.entity.anim.AnimationDriver;
import com.azure.pathogenesis.registry.PathogenEntities;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class NeophyteEntity extends MaturingPathogenMob<NeophyteEntity, NeophyteGoal> implements HearingMob, ScentFollower {

    private final HearingState<NeophyteEntity> hearing = new HearingState<>(this);

    private final AnimationDriver animations = new AnimationDriver("neophyte");

    private final ScentTracker scent = new ScentTracker();

    public NeophyteEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        this.probes.add(agent -> CortexGlue.healthFraction(agent) <= PathogenBlackboardKeys.RETREAT_HEALTH);
        this.runtime.addPeriodicHook("neophyte_scent", 40, (agent, blackboard) -> {
            if (
                blackboard.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) instanceof PathogenGoal goal && goal.isPassive()
            ) {
                agent.scent.sniff(agent);
            }
        });
    }

    @Override
    protected TargetSensor<NeophyteEntity> createSensor() {
        return new TargetSensor<>(
            TargetSensor.nearestMatching(
                Pathogenesis.getConfig().entityConfigs.neophyteConfigs.neophyteHostileRange,
                this::isValidPrey
            ),
            10,
            TargetSensor.lineOfSight()
        );
    }

    @Override
    protected BehaviorNode<NeophyteEntity, NeophyteGoal> createTree() {
        return NeophyteTree.create();
    }

    @Override
    protected GoalPlanner<NeophyteEntity, NeophyteGoal> createPlanner() {
        return new NeophyteGoalPlanner();
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
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected int growthTime() {
        return Pathogenesis.getConfig().lifecycleConfigs.neophyteGrowthTime;
    }

    @Override
    protected float minScale() {
        return 0.8F;
    }

    @Override
    protected float maxScale() {
        return 1.1F;
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
    public HearingState<NeophyteEntity> hearing() {
        return hearing;
    }

    @Override
    public ScentTracker scent() {
        return scent;
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
        if (tickGrowth((ServerLevel) level())) {
            return;
        }
        tickBrain(() -> runtime.getBlackboard().has(CommonBlackboardKeys.TARGET) || scent.consumeNew());
        animations.tickLoop(this, locomotionAnimation());
    }

    private String locomotionAnimation() {
        var speedSqr = getDeltaMovement().horizontalDistanceSqr();
        if (speedSqr < 1.0E-4D) {
            return "idle";
        }
        return speedSqr > 0.025D ? "run" : "walk";
    }

    @Override
    public void mature(ServerLevel level) {
        transformInto(
            level,
            PathogenEntities.NEOMORPH.get(),
            0.8D,
            0.6D,
            PathogenSounds.NEOMORPH_SCREECH.get(),
            1.5F,
            1.1F
        );
    }

    @Override
    public boolean killedEntity(@NotNull ServerLevel level, @NotNull LivingEntity entity) {
        if (entity.getType().is(PathogenTags.Entities.NEOMORPH_TARGETS) || entity instanceof Player) {
            feed(level, entity, 4.0F);
        }
        return super.killedEntity(level, entity);
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
}
