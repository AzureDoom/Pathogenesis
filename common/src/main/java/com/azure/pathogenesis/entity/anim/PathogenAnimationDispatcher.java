package com.azure.pathogenesis.entity.anim;

import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public class PathogenAnimationDispatcher {

    public static final String CONTROLLER = "base_controller";

    private final Mob entity;

    private final double runThresholdSqr;

    private String locomotion = "";

    private int oneShotTicks;

    public PathogenAnimationDispatcher(Mob entity, double runSpeed) {
        this.entity = entity;
        this.runThresholdSqr = runSpeed * runSpeed;
    }

    public void tick() {
        if (oneShotTicks > 0) {
            oneShotTicks--;
            return;
        }
        var speedSqr = entity.getDeltaMovement().horizontalDistanceSqr();
        var next = speedSqr < 1.0E-4D ? "idle" : speedSqr > runThresholdSqr ? "run" : "walk";
        if (entity.isAggressive() && !next.equals("idle")) {
            next = "run";
        }
        if (!next.equals(locomotion)) {
            locomotion = next;
            send(entity, next, false);
        }
    }

    public void playOnce(String animation, int holdTicks) {
        oneShotTicks = holdTicks;
        locomotion = "";
        send(entity, animation, true);
    }

    private static void send(Entity entity, String animation, boolean once) {
        AzCommand.create(CONTROLLER, animation, once ? AzPlayBehaviors.PLAY_ONCE : AzPlayBehaviors.LOOP)
            .sendForEntity(entity);
    }
}
