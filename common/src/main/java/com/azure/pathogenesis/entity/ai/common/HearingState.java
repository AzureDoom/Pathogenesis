package com.azure.pathogenesis.entity.ai.common;

import com.azure.pathogenesis.entity.ai.neomorph.NeomorphHearing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import org.jetbrains.annotations.Nullable;

public final class HearingState<E extends Mob & SoundListener> {

    private final E owner;

    private final DynamicGameEventListener<NeomorphHearing<E>> listener;

    @Nullable
    private BlockPos heardPos;

    private long heardTick;

    private boolean fresh;

    public HearingState(E owner) {
        this.owner = owner;
        this.listener = new DynamicGameEventListener<>(new NeomorphHearing<>(owner));
    }

    public DynamicGameEventListener<NeomorphHearing<E>> listener() {
        return listener;
    }

    public void hear(BlockPos pos) {
        this.heardPos = pos.immutable();
        this.heardTick = owner.level().getGameTime();
        this.fresh = true;
    }

    @Nullable
    public BlockPos heardPos() {
        return heardPos;
    }

    public long heardTick() {
        return heardTick;
    }

    public void clear() {
        heardPos = null;
    }

    public boolean consumeFresh() {
        var wasFresh = fresh;
        fresh = false;
        return wasFresh;
    }
}
