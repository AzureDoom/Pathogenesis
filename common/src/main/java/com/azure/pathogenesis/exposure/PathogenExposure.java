package com.azure.pathogenesis.exposure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public final class PathogenExposure {

    public static final int MAX = 200;

    public static final int CONTACT_CEILING = 60;

    private static final int DECAY_GRACE_TICKS = 600;

    private static final int DECAY_INTERVAL_TICKS = 40;

    private int exposure;

    private long lastExposureTick;

    private long lastContactTick = -1000L;

    private ExposureType lastType = ExposureType.ENVIRONMENTAL;

    public void add(int amount, ExposureType type, long gameTime) {
        var ceiling = type == ExposureType.CONTACT || type == ExposureType.ENVIRONMENTAL ? CONTACT_CEILING : MAX;
        if (exposure < ceiling) {
            exposure = Mth.clamp(exposure + amount, 0, ceiling);
        }
        lastExposureTick = gameTime;
        lastType = type;
    }

    public boolean tryContact(long gameTime) {
        if (gameTime - lastContactTick < 20) {
            return false;
        }
        lastContactTick = gameTime;
        return true;
    }

    public void decay(long gameTime) {
        if (exposure > 0 && gameTime - lastExposureTick > DECAY_GRACE_TICKS && gameTime % DECAY_INTERVAL_TICKS == 0) {
            exposure--;
        }
    }

    public boolean isEmpty() {
        return exposure <= 0;
    }

    public int exposure() {
        return exposure;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("Exposure", exposure);
        tag.putLong("Last", lastExposureTick);
        tag.putString("Type", lastType.id());
        return tag;
    }

    public static PathogenExposure load(CompoundTag tag) {
        var exposure = new PathogenExposure();
        exposure.exposure = Mth.clamp(tag.getInt("Exposure"), 0, MAX);
        exposure.lastExposureTick = tag.getLong("Last");
        exposure.lastType = ExposureType.byId(tag.getString("Type"));
        return exposure;
    }
}
