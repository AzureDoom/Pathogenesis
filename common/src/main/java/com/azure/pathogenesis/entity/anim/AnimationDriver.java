package com.azure.pathogenesis.entity.anim;

import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public final class AnimationDriver {

    public static final String BASE_CONTROLLER = "base_controller";

    private final String prefix;

    private final Map<String, AzCommand> loops = new HashMap<>();

    private final Map<String, AzCommand> once = new HashMap<>();

    private String current = "";

    private int lockTicks;

    public AnimationDriver(String entityName) {
        this.prefix = "animation." + entityName + ".";
    }

    private AzCommand loopCommand(String name) {
        return loops.computeIfAbsent(name, n -> AzCommand.create(BASE_CONTROLLER, prefix + n, AzPlayBehaviors.LOOP));
    }

    private AzCommand onceCommand(String name) {
        return once.computeIfAbsent(
            name,
            n -> AzCommand.create(BASE_CONTROLLER, prefix + n, AzPlayBehaviors.PLAY_ONCE)
        );
    }

    public void tickLoop(Entity entity, String name) {
        if (lockTicks > 0) {
            lockTicks--;
            return;
        }
        if (!name.equals(current)) {
            current = name;
            loopCommand(name).sendForEntity(entity);
        }
    }

    public void playOnce(Entity entity, String name, int holdTicks) {
        onceCommand(name).sendForEntity(entity);
        lockTicks = holdTicks;
        current = "";
    }
}
