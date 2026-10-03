package com.azure.pathogenesis.blockentity;

import com.azure.pathogenesis.registry.PathogenBlockEntities;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SporePlantBlockEntity extends BlockEntity {

    public static final String CONTROLLER = "base_controller";

    private static final AzCommand SPRAY = AzCommand.create(CONTROLLER, "spray", AzPlayBehaviors.PLAY_ONCE);

    private final Map<String, Vec3> emitters = new LinkedHashMap<>();

    public SporePlantBlockEntity(BlockPos pos, BlockState state) {
        super(PathogenBlockEntities.SPORE_PLANT.get(), pos, state);
    }

    public void playSpray() {
        if (level instanceof ServerLevel) {
            SPRAY.sendForBlockEntity(this);
        }
    }

    public void setEmitter(String bone, Vec3 blockLocal) {
        emitters.put(bone, blockLocal);
    }

    public List<Vec3> emitterPositions() {
        var origin = Vec3.atLowerCornerOf(worldPosition);
        var out = new ArrayList<Vec3>(emitters.size());
        for (var local : emitters.values()) {
            out.add(origin.add(local));
        }
        return out;
    }
}
