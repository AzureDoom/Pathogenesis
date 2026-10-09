package com.azure.pathogenesis.entity;

import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.GoalPlanner;
import com.azure.azurecortex.sensing.TargetSensor;
import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.entity.ai.bloodburster.BloodbursterGoal;
import com.azure.pathogenesis.entity.ai.bloodburster.BloodbursterGoalPlanner;
import com.azure.pathogenesis.entity.ai.bloodburster.BloodbursterTree;
import com.azure.pathogenesis.entity.ai.common.PathogenBlackboardKeys;
import com.azure.pathogenesis.entity.ai.common.PathogenGoal;
import com.azure.pathogenesis.entity.ai.common.ScentFollower;
import com.azure.pathogenesis.entity.ai.common.ScentTracker;
import com.azure.pathogenesis.entity.anim.PathogenAnimationDispatcher;
import com.azure.pathogenesis.registry.PathogenEntities;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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

public class BloodbursterEntity extends MaturingPathogenMob<BloodbursterEntity, BloodbursterGoal> implements ScentFollower {

    private final PathogenAnimationDispatcher animations = new PathogenAnimationDispatcher(this, 0.12D);

    private final ScentTracker scent = new ScentTracker();

    private int emergeTicks;

    public BloodbursterEntity(EntityType<? extends BloodbursterEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.probes.add(agent -> agent.runtime.getBlackboard().get(PathogenBlackboardKeys.THREAT) != null);
        this.runtime.addPeriodicHook(
            "bloodburster_threat_scan",
            10,
            (agent, blackboard) -> blackboard.set(PathogenBlackboardKeys.THREAT, agent.nearestThreat())
        );
        this.runtime.addPeriodicHook("bloodburster_scent", 40, (agent, blackboard) -> {
            var goal = blackboard.get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE);
            if ((goal == null || goal instanceof PathogenGoal g && g.isPassive()) && !agent.isExposed()) {
                agent.scent.sniff(agent);
            }
        });
    }

    @Override
    protected TargetSensor<BloodbursterEntity> createSensor() {
        return new TargetSensor<>(
            TargetSensor.nearestMatching(
                Pathogenesis.getConfig().entityConfigs.bloodbursterConfigs.bloodbursterHostileRange,
                this::isPrey
            ),
            10,
            TargetSensor.lineOfSight()
        );
    }

    @Override
    protected BehaviorNode<BloodbursterEntity, BloodbursterGoal> createTree() {
        return BloodbursterTree.create();
    }

    @Override
    protected GoalPlanner<BloodbursterEntity, BloodbursterGoal> createPlanner() {
        return new BloodbursterGoalPlanner();
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
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected int growthTime() {
        return Pathogenesis.getConfig().lifecycleConfigs.bloodbursterGrowthTime;
    }

    @Override
    protected float minScale() {
        return 0.55F;
    }

    @Override
    protected float maxScale() {
        return 1.35F;
    }

    public void markEmerged() {
        this.emergeTicks = 20;
    }

    public PathogenAnimationDispatcher animations() {
        return animations;
    }

    @Override
    public ScentTracker scent() {
        return scent;
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
        if (tickGrowth((ServerLevel) level())) {
            return;
        }
        tickBrain(() -> {
            var blackboard = runtime.getBlackboard();
            return blackboard.get(PathogenBlackboardKeys.THREAT) != null
                || isHungry() && blackboard.get(CommonBlackboardKeys.TARGET) != null
                || scent.consumeNew();
        });
        animations.tick();
    }

    @Override
    public void mature(ServerLevel level) {
        transformInto(
            level,
            PathogenEntities.NEOPHYTE.get(),
            0.3D,
            0.3D,
            PathogenSounds.BLOODBURSTER_MATURE.get(),
            1.2F,
            1.0F
        );
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
            feed(level, entity, 2.0F);
        }
        return super.killedEntity(level, entity);
    }

    @Override
    public boolean isPreventingPlayerRest(@NotNull Player player) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return PathogenSounds.BLOODBURSTER_AMBIENT.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return PathogenSounds.BLOODBURSTER_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return PathogenSounds.BLOODBURSTER_DEATH.get();
    }
}
