package com.azure.pathogenesis.entity.ai.neomorph;

import com.azure.pathogenesis.entity.ai.common.SoundListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class NeomorphHearing<E extends Mob & SoundListener> implements GameEventListener {

    public static final int RADIUS = 20;

    private final E owner;

    private final PositionSource source;

    public NeomorphHearing(E owner) {
        this.owner = owner;
        this.source = new EntityPositionSource(owner, owner.getEyeHeight());
    }

    @Override
    public @NotNull PositionSource getListenerSource() {
        return source;
    }

    @Override
    public int getListenerRadius() {
        return RADIUS;
    }

    @Override
    public boolean handleGameEvent(
        @NotNull ServerLevel level,
        @NotNull Holder<GameEvent> event,
        GameEvent.@NotNull Context context,
        @NotNull Vec3 pos
    ) {
        if (!owner.isAlive() || owner.isNoAi() || !event.is(GameEventTags.VIBRATIONS)) {
            return false;
        }
        var cause = context.sourceEntity();
        if (cause == owner || cause instanceof SoundListener) {
            return false;
        }
        if (cause != null && cause.isSteppingCarefully() && event.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
            return false;
        }
        owner.onHeard(BlockPos.containing(pos), cause);
        return true;
    }
}
