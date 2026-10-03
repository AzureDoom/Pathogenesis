package com.azure.pathogenesis.entity.ai.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class DarkBiasedWanderAction<E extends PathfinderMob, G> extends RoamAction<E, G> {

    private final int range;

    public DarkBiasedWanderAction(String name, int priority, double speed, int range, int maxDuration) {
        super(name, priority, speed, range, maxDuration);
        this.range = range;
    }

    @Override
    protected Vec3 findDestination(E agent) {
        Vec3 best = null;
        var bestLight = Integer.MAX_VALUE;
        for (var i = 0; i < 4; i++) {
            var candidate = DefaultRandomPos.getPos(agent, range, 4);
            if (candidate == null) {
                continue;
            }
            var light = agent.level().getMaxLocalRawBrightness(BlockPos.containing(candidate));
            if (light < bestLight) {
                bestLight = light;
                best = candidate;
            }
        }
        return best;
    }
}
