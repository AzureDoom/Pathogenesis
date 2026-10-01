package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.Pathogenesis;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PathogenSavedData extends SavedData {

    private static final String NAME = Pathogenesis.MOD_ID + "_zones";

    private final Map<UUID, PathogenZone> zones = new LinkedHashMap<>();

    private List<PathogenZone> snapshot = List.of();

    @SuppressWarnings("DataFlowIssue")
    public static PathogenSavedData get(ServerLevel level) {
        return level.getDataStorage()
            .computeIfAbsent(new SavedData.Factory<>(PathogenSavedData::new, PathogenSavedData::load, null), NAME);
    }

    private static PathogenSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new PathogenSavedData();
        var list = tag.getList("Zones", Tag.TAG_COMPOUND);
        for (var i = 0; i < list.size(); i++) {
            var zone = PathogenZone.load(list.getCompound(i));
            data.zones.put(zone.id(), zone);
        }
        data.rebuildSnapshot();
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        var list = new ListTag();
        zones.values().forEach(zone -> list.add(zone.save()));
        tag.put("Zones", list);
        return tag;
    }

    public boolean isEmpty() {
        return zones.isEmpty();
    }

    public List<PathogenZone> zoneList() {
        return snapshot;
    }

    @Nullable
    public PathogenZone get(@Nullable UUID id) {
        return id == null ? null : zones.get(id);
    }

    public void add(PathogenZone zone) {
        zones.put(zone.id(), zone);
        rebuildSnapshot();
        setDirty();
    }

    public void remove(UUID id) {
        if (zones.remove(id) != null) {
            rebuildSnapshot();
            setDirty();
        }
    }

    private void rebuildSnapshot() {
        snapshot = List.copyOf(zones.values());
    }
}
