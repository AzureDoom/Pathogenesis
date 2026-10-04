package com.azure.pathogenesis.entity.ai.common;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.contamination.CarcassSites;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ScentTracker {

    private final Map<BlockPos, Long> investigated = new LinkedHashMap<>();

    @Nullable
    private BlockPos target;

    private long acquiredTick;

    private boolean fresh;

    public void sniff(PathfinderMob agent) {
        if (target != null || !(agent.level() instanceof ServerLevel level)) {
            return;
        }
        var config = Pathogenesis.getConfig().carcassConfigs;
        if (!config.predatorScentEnabled || agent.getRandom().nextDouble() >= config.predatorScentChance) {
            return;
        }
        var now = level.getGameTime();
        investigated.values().removeIf(until -> until <= now);
        var found = CarcassSites.findScent(
            level,
            agent.blockPosition(),
            config.predatorScentRange,
            4.0D,
            investigated::containsKey
        );
        if (found != null) {
            target = found;
            acquiredTick = now;
            fresh = true;
        }
    }

    @Nullable
    public BlockPos target() {
        return target;
    }

    public long acquiredTick() {
        return acquiredTick;
    }

    public boolean consumeNew() {
        var wasFresh = fresh;
        fresh = false;
        return wasFresh;
    }

    public void markInvestigated(long now) {
        if (target != null) {
            investigated.remove(target);
            investigated.put(target, now + 6000L);
            while (investigated.size() > 8) {
                investigated.remove(investigated.keySet().iterator().next());
            }
        }
        clear();
    }

    public void clear() {
        target = null;
        fresh = false;
    }
}
