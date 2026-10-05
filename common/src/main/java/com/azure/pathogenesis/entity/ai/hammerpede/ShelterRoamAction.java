package com.azure.pathogenesis.entity.ai.hammerpede;

import com.azure.pathogenesis.entity.HammerpedeEntity;
import com.azure.pathogenesis.entity.ai.common.RoamAction;
import net.minecraft.world.phys.Vec3;

public class ShelterRoamAction extends RoamAction<HammerpedeEntity, HammerpedeGoal> {

    private final int range;

    public ShelterRoamAction(String name, int priority, double speed, int range, int maxDuration) {
        super(name, priority, speed, range, maxDuration);
        this.range = range;
    }

    @Override
    protected Vec3 findDestination(HammerpedeEntity agent) {
        return HideAction.findSpot(agent, range, 6);
    }
}
