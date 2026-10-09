package com.azure.pathogenesis.entity;

import com.azure.azurecortex.api.behavior.BehaviorNode;
import com.azure.azurecortex.api.blackboard.CommonBlackboardKeys;
import com.azure.azurecortex.goap.EmergencyDetector;
import com.azure.azurecortex.goap.GoalPlanner;
import com.azure.azurecortex.runtime.CortexRuntime;
import com.azure.azurecortex.sensing.TargetSensor;
import com.azure.pathogenesis.entity.ai.common.CortexGlue;
import com.azure.pathogenesis.entity.ai.common.HearingMob;
import com.azure.pathogenesis.entity.ai.common.PathogenGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * Shared base for every Pathogenesis mob: AzureCortex wiring, origin zone tracking and persistence.
 *
 * @param <SELF> the concrete entity class
 * @param <G>    that entity's goal enum
 */
public abstract class PathogenMob<SELF extends PathogenMob<SELF, G>, G extends PathogenGoal> extends Monster {

    protected final CortexRuntime<SELF, G> runtime;

    protected final GoalPlanner<SELF, G> planner;

    protected final List<EmergencyDetector.EmergencyProbe<SELF>> probes;

    @Nullable
    protected UUID originZone;

    protected PathogenMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.planner = createPlanner();
        this.runtime = new CortexRuntime<>(self(), createSensor(), createTree());
        this.probes = new ArrayList<>(EmergencyDetector.defaultProbes());
    }

    protected abstract TargetSensor<SELF> createSensor();

    protected abstract BehaviorNode<SELF, G> createTree();

    protected abstract GoalPlanner<SELF, G> createPlanner();

    @SuppressWarnings("unchecked")
    protected final SELF self() {
        return (SELF) this;
    }

    protected void tickBrain(BooleanSupplier reactive) {
        if (isNoAi()) {
            return;
        }
        CortexGlue.tickPlanner(
            self(),
            runtime,
            planner,
            probes,
            goal -> goal instanceof PathogenGoal g && g.isPassive(),
            reactive.getAsBoolean()
        );
        runtime.tick();
    }

    @Nullable
    public Entity retreatFrom() {
        var target = runtime.getBlackboard().get(CommonBlackboardKeys.TARGET);
        return target != null ? target : getLastHurtByMob();
    }

    public void setOriginZone(@Nullable UUID zone) {
        this.originZone = zone;
    }

    @Nullable
    public UUID originZone() {
        return originZone;
    }

    @Override
    public void updateDynamicGameEventListener(@NotNull BiConsumer<DynamicGameEventListener<?>, ServerLevel> consumer) {
        if (this instanceof HearingMob mob && level() instanceof ServerLevel serverLevel) {
            consumer.accept(mob.hearing().listener(), serverLevel);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
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
