package com.azure.pathogenesis.infection;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.exposure.PathogenExposure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public final class PathogenHostSavedData extends SavedData {

    private static final String NAME = Pathogenesis.MOD_ID + "_hosts";

    @SuppressWarnings("DataFlowIssue")
    public static PathogenHostSavedData get(ServerLevel level) {
        return level.getServer()
            .overworld()
            .getDataStorage()
            .computeIfAbsent(
                new SavedData.Factory<>(PathogenHostSavedData::createEmpty, PathogenHostSavedData::load, null),
                NAME
            );
    }

    private static PathogenHostSavedData createEmpty() {
        PathogenHosts.clearAll();
        return new PathogenHostSavedData();
    }

    private static PathogenHostSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PathogenHosts.clearAll();
        var list = tag.getList("Hosts", Tag.TAG_COMPOUND);
        for (var i = 0; i < list.size(); i++) {
            var compound = list.getCompound(i);
            if (!compound.hasUUID("Id")) {
                continue;
            }
            var state = new HostState();
            if (compound.contains("Exposure", Tag.TAG_COMPOUND)) {
                state.setExposure(PathogenExposure.load(compound.getCompound("Exposure")));
            }
            if (compound.contains("Infection", Tag.TAG_COMPOUND)) {
                state.setInfection(NeomorphInfection.load(compound.getCompound("Infection")));
            }
            if (state.isEmpty()) {
                continue;
            }
            state.lastKnownPos = BlockPos.of(compound.getLong("Pos"));
            if (compound.contains("Dimension", Tag.TAG_STRING)) {
                var location = ResourceLocation.tryParse(compound.getString("Dimension"));
                state.dimension = location == null ? null : ResourceKey.create(Registries.DIMENSION, location);
            }
            state.isPlayer = compound.getBoolean("Player");
            PathogenHosts.restore(compound.getUUID("Id"), state);
        }
        return new PathogenHostSavedData();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        var list = new ListTag();
        for (var entry : PathogenHosts.snapshotForSave().entrySet()) {
            var state = entry.getValue();
            if (state.isEmpty()) {
                continue;
            }
            var compound = new CompoundTag();
            compound.putUUID("Id", entry.getKey());
            var exposure = state.exposure();
            if (exposure != null) {
                compound.put("Exposure", exposure.save());
            }
            var infection = state.infection();
            if (infection != null) {
                compound.put("Infection", infection.save());
            }
            compound.putLong("Pos", state.lastKnownPos.asLong());
            if (state.dimension != null) {
                compound.putString("Dimension", state.dimension.location().toString());
            }
            compound.putBoolean("Player", state.isPlayer);
            list.add(compound);
        }
        tag.put("Hosts", list);
        return tag;
    }
}
