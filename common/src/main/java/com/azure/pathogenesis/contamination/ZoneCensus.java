package com.azure.pathogenesis.contamination;

import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

final class ZoneCensus {

    private static final Predicate<BlockState> IS_CONTAMINATED = state -> state.is(PathogenTags.Blocks.CONTAMINATED);

    private static boolean isFlora(BlockState state) {
        return state.is(PathogenTags.Blocks.PATHOGEN_GROWTH) || state.is(PathogenTags.Blocks.SPORE_PLANTS);
    }

    final UUID zoneId;

    final long startedTick;

    private final List<ChunkPos> chunks = new ArrayList<>();

    private final int minSectionY;

    private final int maxSectionY;

    private int chunkIndex;

    private int sectionY;

    private int count;

    private int flora;

    private boolean aborted;

    ZoneCensus(ServerLevel level, PathogenZone zone) {
        this.zoneId = zone.id();
        this.startedTick = level.getGameTime();
        var reach = zone.radius() + PathogenZone.EDGE_MARGIN;
        var minCx = SectionPos.blockToSectionCoord(zone.origin().getX() - reach);
        var maxCx = SectionPos.blockToSectionCoord(zone.origin().getX() + reach);
        var minCz = SectionPos.blockToSectionCoord(zone.origin().getZ() - reach);
        var maxCz = SectionPos.blockToSectionCoord(zone.origin().getZ() + reach);
        for (var cx = minCx; cx <= maxCx; cx++) {
            for (var cz = minCz; cz <= maxCz; cz++) {
                chunks.add(new ChunkPos(cx, cz));
            }
        }
        var originSection = SectionPos.blockToSectionCoord(zone.origin().getY());
        var reachSections = SectionPos.blockToSectionCoord(PathogenZone.VERTICAL_REACH) + 1;
        this.minSectionY = Math.max(level.getMinSection(), originSection - reachSections);
        this.maxSectionY = Math.min(level.getMaxSection() - 1, originSection + reachSections);
        this.sectionY = minSectionY;
    }

    boolean step(ServerLevel level, int budget) {
        while (budget > 0 && chunkIndex < chunks.size()) {
            var pos = chunks.get(chunkIndex);
            var chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null) {
                aborted = true;
                return true;
            }
            if (sectionY > maxSectionY) {
                chunkIndex++;
                sectionY = minSectionY;
                continue;
            }
            var section = chunk.getSection(level.getSectionIndexFromSectionY(sectionY++));
            budget--;
            if (section.hasOnlyAir() || !section.maybeHas(IS_CONTAMINATED)) {
                continue;
            }
            for (var y = 0; y < 16; y++) {
                for (var z = 0; z < 16; z++) {
                    for (var x = 0; x < 16; x++) {
                        var state = section.getBlockState(x, y, z);
                        if (IS_CONTAMINATED.test(state)) {
                            count++;
                            if (isFlora(state)) {
                                flora++;
                            }
                        }
                    }
                }
            }
            budget = 0;
        }
        return chunkIndex >= chunks.size();
    }

    boolean aborted() {
        return aborted;
    }

    int count() {
        return count;
    }

    int flora() {
        return flora;
    }
}
