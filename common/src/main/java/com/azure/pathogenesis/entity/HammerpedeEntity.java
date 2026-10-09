package com.azure.pathogenesis.entity;

import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.GoalPlanner;
import com.azure.azurecortex.sensing.TargetSensor;
import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.compat.OvomorphosisCompat;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.entity.ai.common.HearingMob;
import com.azure.pathogenesis.entity.ai.common.HearingState;
import com.azure.pathogenesis.entity.ai.common.SoundListener;
import com.azure.pathogenesis.entity.ai.hammerpede.HammerpedeGoal;
import com.azure.pathogenesis.entity.ai.hammerpede.HammerpedeGoalPlanner;
import com.azure.pathogenesis.entity.ai.hammerpede.HammerpedeTree;
import com.azure.pathogenesis.entity.anim.AnimationDriver;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BushBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HammerpedeEntity extends PathogenMob<HammerpedeEntity, HammerpedeGoal> implements HearingMob {

    private final HearingState<HammerpedeEntity> hearing = new HearingState<>(this);

    private final AnimationDriver animations = new AnimationDriver("hammerpede");

    private int retreatTicks;

    private int acidCooldown;

    private int emergeTicks;

    public HammerpedeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 3;
        this.probes.add(
            agent -> agent.isRetreating()
                && agent.runtime.getBlackboard().get(CommonBlackboardKeys.ACTIVE_GOAL_TYPE) != HammerpedeGoal.RETREAT
        );
    }

    @Override
    protected TargetSensor<HammerpedeEntity> createSensor() {
        return new TargetSensor<>(
            TargetSensor.nearestMatching(
                Pathogenesis.getConfig().entityConfigs.hammerpedeConfigs.hammerpedeHostileRange,
                this::isNoticedPrey
            ),
            10,
            TargetSensor.lineOfSight()
        );
    }

    @Override
    protected BehaviorNode<HammerpedeEntity, HammerpedeGoal> createTree() {
        return HammerpedeTree.create();
    }

    @Override
    protected GoalPlanner<HammerpedeEntity, HammerpedeGoal> createPlanner() {
        return new HammerpedeGoalPlanner();
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = Pathogenesis.getConfig().entityConfigs.hammerpedeConfigs;
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, config.hammerpedeHealth)
            .add(Attributes.ARMOR, config.hammerpedeArmor)
            .add(Attributes.MOVEMENT_SPEED, config.hammerpedeMovementSpeed)
            .add(Attributes.ATTACK_DAMAGE, config.hammerpedeAttackDamage)
            .add(Attributes.FOLLOW_RANGE, config.hammerpedeHostileRange * 1.25D)
            .add(Attributes.STEP_HEIGHT, 1.0D)
            .add(Attributes.SAFE_FALL_DISTANCE, 5.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    public static int hidingScore(Level level, BlockPos pos) {
        var score = 0;
        var feet = level.getBlockState(pos);
        if (feet.getBlock() instanceof BushBlock) {
            score += 3;
        }
        if (isShallowWater(level, pos)) {
            score += 3;
        }
        if (!level.canSeeSky(pos)) {
            score += 2;
        }
        if (level.getBrightness(LightLayer.BLOCK, pos) < 8 && (!level.canSeeSky(pos) || !level.isDay())) {
            score += 1;
        }
        var below = level.getBlockState(pos.below());
        if (below.is(PathogenTags.Blocks.CONTAMINATED) || feet.is(PathogenTags.Blocks.CONTAMINATED)) {
            score += 2;
        }
        return score;
    }

    private static boolean isShallowWater(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER) && !level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    public boolean isHidden() {
        var level = level();
        var pos = blockPosition();
        return level.getBlockState(pos).getBlock() instanceof BushBlock || isShallowWater(level, pos)
            || !level.canSeeSky(pos);
    }

    public boolean isValidPrey(LivingEntity entity) {
        if (entity == this || !entity.isAlive() || entity instanceof SoundListener) {
            return false;
        }
        if (entity.getType().is(PathogenTags.Entities.ALIEN_ORGANISMS)) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return entity.getType().is(PathogenTags.Entities.NEOMORPH_TARGETS)
            || entity.getType().is(PathogenTags.Entities.BLOODBURSTER_PREY);
    }

    private boolean isNoticedPrey(LivingEntity entity) {
        if (!isValidPrey(entity)) {
            return false;
        }
        if (entity == getLastHurtByMob() || distanceToSqr(entity) <= 4.0D * 4.0D) {
            return true;
        }
        var dx = entity.getX() - entity.xo;
        var dz = entity.getZ() - entity.zo;
        return dx * dx + dz * dz > 1.0E-4D && !entity.isSteppingCarefully();
    }

    public boolean isRetreating() {
        return retreatTicks > 0;
    }

    public int retreatTicks() {
        return retreatTicks;
    }

    public void beginRetreat(int ticks) {
        retreatTicks = Math.max(retreatTicks, ticks);
    }

    public void markEmerged() {
        emergeTicks = 20;
    }

    public void onBite(LivingEntity target) {
        animations.playOnce(this, "bite", 8);
        playSound(PathogenSounds.BLOODBURSTER_HURT.get(), 0.8F, 1.5F + random.nextFloat() * 0.2F);
    }

    public void onLungeWindup() {
        animations.playOnce(this, "lunge", 14);
    }

    public void onLungeLaunch() {
        playSound(PathogenSounds.BLOODBURSTER_AMBIENT.get(), 1.0F, 1.6F);
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        var hit = super.doHurtTarget(target);
        if (hit) {
            var config = Pathogenesis.getConfig().entityConfigs.hammerpedeConfigs;
            if (random.nextDouble() < config.hammerpedeRetreatChance) {
                beginRetreat(config.hammerpedeRetreatTicks);
            }
        }
        return hit;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        var hurt = super.hurt(source, amount);
        if (hurt && level() instanceof ServerLevel serverLevel && isAlive()) {
            if (source.getEntity() instanceof LivingEntity attacker && isValidPrey(attacker)) {
                runtime.getBlackboard().set(CommonBlackboardKeys.TARGET, attacker);
            }
            var config = Pathogenesis.getConfig().entityConfigs.hammerpedeConfigs;
            if (amount > 0.0F && acidCooldown <= 0 && random.nextDouble() < config.hammerpedeAcidChance) {
                acidCooldown = 20;
                OvomorphosisCompat.spawnAcid(this, serverLevel, position());
            }
        }
        return hurt;
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel serverLevel) {
            OvomorphosisCompat.spawnAcid(
                this,
                serverLevel,
                position().add((random.nextDouble() - 0.5D) * 1.2D, 0.0D, (random.nextDouble() - 0.5D) * 1.2D)
            );
        }
    }

    @Override
    public HearingState<HammerpedeEntity> hearing() {
        return hearing;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        var level = (ServerLevel) level();
        if (emergeTicks > 0) {
            if (emergeTicks-- == 20) {
                animations.playOnce(this, "emerge", 0);
            }
            getNavigation().stop();
            return;
        }
        if (retreatTicks > 0) {
            retreatTicks--;
        }
        if (acidCooldown > 0) {
            acidCooldown--;
        }
        if (tickCount % 100 == 0) {
            trailContamination(level);
        }
        tickBrain(() -> {
            var heard = hearing.consumeFresh();
            return runtime.getBlackboard().has(CommonBlackboardKeys.TARGET) || heard;
        });
        animations.tickLoop(this, locomotionAnimation());
    }

    private void trailContamination(ServerLevel level) {
        var config = Pathogenesis.getConfig();
        if (!onGround() || random.nextDouble() >= config.entityConfigs.hammerpedeConfigs.hammerpedeTrailChance) {
            return;
        }
        var zone = PathogenZoneManager.findZone(level, blockPosition());
        if (zone == null) {
            return;
        }
        PathogenZoneManager.contaminate(
            level,
            zone,
            blockPosition().below(),
            config.contaminationConfigs.pathogenMaxRadius
        );
    }

    private String locomotionAnimation() {
        var speedSqr = getDeltaMovement().horizontalDistanceSqr();
        if (speedSqr < 1.0E-4D) {
            return isHidden() ? "lurk" : "idle";
        }
        return speedSqr > 0.03D || isRetreating() ? "run" : "walk";
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() + 0.5F;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isHidden() ? null : PathogenSounds.BLOODBURSTER_AMBIENT.get();
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return PathogenSounds.BLOODBURSTER_HURT.get();
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return PathogenSounds.BLOODBURSTER_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RetreatTicks", retreatTicks);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        retreatTicks = tag.getInt("RetreatTicks");
    }
}
