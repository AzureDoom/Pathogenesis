package com.azure.pathogenesis.blockentity;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.block.PathogenSourceBlock;
import com.azure.pathogenesis.contamination.ContainmentState;
import com.azure.pathogenesis.contamination.PathogenSavedData;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.contamination.RuptureStrength;
import com.azure.pathogenesis.exposure.ExposureType;
import com.azure.pathogenesis.exposure.PathogenExposureHelper;
import com.azure.pathogenesis.registry.PathogenBlockEntities;
import com.azure.pathogenesis.registry.PathogenSounds;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class PathogenSourceBlockEntity extends BlockEntity {

    public static final int CAPACITY = 2400;

    public static final String CONTROLLER = "base_controller";

    private static final Map<ContainmentState, AzCommand> HOLD_COMMANDS = new EnumMap<>(ContainmentState.class);

    static {
        for (var state : ContainmentState.values()) {
            HOLD_COMMANDS.put(
                state,
                AzCommand.create(CONTROLLER, state.getSerializedName(), AzPlayBehaviors.HOLD_ON_LAST_FRAME)
            );
        }
    }

    private static final int DAMAGE_DELAY = 200;

    private int pathogen = CAPACITY;

    private int damageTimer = DAMAGE_DELAY;

    private int drainTicks;

    private boolean openWarned;

    @Nullable
    private UUID zoneId;

    @Nullable
    private ContainmentState animatedState;

    public PathogenSourceBlockEntity(BlockPos pos, BlockState state) {
        super(PathogenBlockEntities.PATHOGEN_SOURCE.get(), pos, state);
    }

    @Nullable
    public UUID zoneId() {
        return zoneId;
    }

    public static AzCommand holdCommand(ContainmentState state) {
        return HOLD_COMMANDS.get(state);
    }

    public void syncAnimation() {
        if (level == null || !level.isClientSide()) {
            return;
        }
        var state = getBlockState().getValue(PathogenSourceBlock.CONTAINMENT);
        if (state != animatedState) {
            animatedState = state;
            holdCommand(state).sendForBlockEntity(this);
        }
    }

    public int pathogen() {
        return pathogen;
    }

    public boolean warnOpenAttempt() {
        if (openWarned) {
            return false;
        }
        openWarned = true;
        setChanged();
        return true;
    }

    public void beginLeak(ServerLevel level, RuptureStrength strength) {
        var zone = PathogenZoneManager.onRupture(level, worldPosition, strength, true);
        zoneId = zone.id();
        if (Pathogenesis.getConfig().debugLogging)
            Pathogenesis.LOGGER.debug(
                    "PathogenSourceBlockEntity.beginLeak zoneId={} strength={} forced={} containment={}",
                    zoneId,
                    strength,
                    zone.isForced(),
                    getBlockState().getValue(PathogenSourceBlock.CONTAINMENT)
            );
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PathogenSourceBlockEntity source) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        var containment = state.getValue(PathogenSourceBlock.CONTAINMENT);
        var time = level.getGameTime();
        switch (containment) {
            case DAMAGED -> {
                if (state.getValue(PathogenSourceBlock.FROZEN)) {
                    return;
                }
                if (--source.damageTimer <= 0) {
                    PathogenSourceBlock.setContainment(level, pos, state, ContainmentState.LEAKING);
                    source.beginLeak(serverLevel, RuptureStrength.CRACK);
                } else if (source.damageTimer % 40 == 0) {
                    level.playSound(null, pos, PathogenSounds.PATHOGEN_LEAK.get(), SoundSource.BLOCKS, 0.3F, 1.4F);
                }
                source.setChanged();
            }
            case LEAKING, OPEN -> source.tickLeak(serverLevel, pos, state, containment == ContainmentState.OPEN, time);
            default -> {}
        }
    }

    private void tickLeak(ServerLevel level, BlockPos pos, BlockState state, boolean open, long time) {
        if (zoneId == null || PathogenSavedData.get(level).get(zoneId) == null) {
            beginLeak(level, open ? RuptureStrength.RUPTURE : RuptureStrength.CRACK);
        }
        var config = Pathogenesis.getConfig().contaminationConfigs;
        var drainInterval = open ? config.openDrainInterval : config.leakDrainInterval;
        if (++drainTicks >= drainInterval) {
            drainTicks = 0;
            pathogen--;
        }
        var center = Vec3.atCenterOf(pos);
        if (time % 20 == 0) {
            PathogenExposureHelper.exposeArea(level, center, open ? 3.5D : 2.5D, open ? 8 : 4, ExposureType.DIRECT);
        }
        if (time % 5 == 0) {
            level.sendParticles(
                PathogenZoneManager.PATHOGEN_DUST,
                center.x,
                center.y + 0.4D,
                center.z,
                open ? 6 : 2,
                0.25D,
                0.2D,
                0.25D,
                0.01D
            );
        }
        if (time % 60 == 0) {
            level.playSound(
                null,
                pos,
                PathogenSounds.PATHOGEN_LEAK.get(),
                SoundSource.BLOCKS,
                open ? 0.9F : 0.5F,
                1.0F
            );
        }
        if (pathogen <= 0) {
            pathogen = 0;
            PathogenSourceBlock.setContainment(level, pos, state, ContainmentState.EMPTY);
            PathogenZoneManager.onSourceInactive(level, zoneId);
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Pathogen", pathogen);
        tag.putInt("DamageTimer", damageTimer);
        tag.putInt("DrainTicks", drainTicks);
        tag.putBoolean("OpenWarned", openWarned);
        if (zoneId != null) {
            tag.putUUID("Zone", zoneId);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        pathogen = tag.contains("Pathogen") ? tag.getInt("Pathogen") : CAPACITY;
        damageTimer = tag.contains("DamageTimer") ? tag.getInt("DamageTimer") : DAMAGE_DELAY;
        drainTicks = tag.getInt("DrainTicks");
        openWarned = tag.getBoolean("OpenWarned");
        zoneId = tag.hasUUID("Zone") ? tag.getUUID("Zone") : null;
    }
}
