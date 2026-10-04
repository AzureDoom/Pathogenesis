package com.azure.pathogenesis.exposure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

public final class PathogenExposure {

    private int exposure;

    private long lastExposureTick;

    private long lastContactTick = -1000L;

    private int lethalTicks = -1;

    private ExposureType lastType = ExposureType.ENVIRONMENTAL;

    public void add(int amount, ExposureType type, long gameTime) {
        var ceiling = type == ExposureType.CONTACT || type == ExposureType.ENVIRONMENTAL ? 60 : 200;
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
        decay(gameTime, false);
    }

    public void decay(long gameTime, boolean washing) {
        if (exposure <= 0) {
            return;
        }
        if (washing) {
            if (gameTime % 8 == 0) {
                exposure--;
            }
            return;
        }
        if (gameTime - lastExposureTick > 600 && gameTime % 40 == 0) {
            exposure--;
        }
    }

    public void reduce(int amount) {
        exposure = Math.max(0, exposure - amount);
    }

    public ExposureTier tier() {
        return ExposureTier.of(exposure);
    }

    public int tickLethal() {
        if (tier() != ExposureTier.EXTREME) {
            lethalTicks = -1;
            return -1;
        }
        if (lethalTicks < 0) {
            lethalTicks = 300;
        }
        lethalTicks--;
        if (lethalTicks <= 0) {
            lethalTicks = -1;
            return 0;
        }
        return lethalTicks;
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
        tag.putInt("Lethal", lethalTicks);
        return tag;
    }

    public static PathogenExposure load(CompoundTag tag) {
        var exposure = new PathogenExposure();
        exposure.exposure = Mth.clamp(tag.getInt("Exposure"), 0, 200);
        exposure.lastExposureTick = tag.getLong("Last");
        exposure.lastType = ExposureType.byId(tag.getString("Type"));
        exposure.lethalTicks = tag.contains("Lethal", Tag.TAG_INT) ? tag.getInt("Lethal") : -1;
        return exposure;
    }
}
