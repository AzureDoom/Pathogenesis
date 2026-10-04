package com.azure.pathogenesis.infection;

import net.minecraft.nbt.CompoundTag;

@SuppressWarnings("unused")
public final class NeomorphInfection {

    private final int totalTicks;

    private final InfectionSite site;

    private int ticks;

    private int treatmentRate;

    private NeomorphInfectionStage stage = NeomorphInfectionStage.EXPOSED;

    public NeomorphInfection(int totalTicks, InfectionSite site) {
        this.totalTicks = Math.max(1, totalTicks);
        this.site = site;
    }

    boolean tick() {
        if (treatmentRate > 0) {
            ticks = Math.max(0, ticks - treatmentRate);
        } else {
            ticks++;
        }
        var next = NeomorphInfectionStage.forProgress(progress());
        if (next != stage) {
            stage = next;
            return true;
        }
        return false;
    }

    public float progress() {
        return ticks / (float) totalTicks;
    }

    public NeomorphInfectionStage stage() {
        return stage;
    }

    public int ticks() {
        return ticks;
    }

    public int totalTicks() {
        return totalTicks;
    }

    public InfectionSite site() {
        return site;
    }

    public boolean isTreating() {
        return treatmentRate > 0;
    }

    public boolean isCured() {
        return treatmentRate > 0 && ticks <= 0;
    }

    public void beginTreatment(int rate) {
        treatmentRate = Math.max(treatmentRate, rate);
    }

    public void advanceTo(NeomorphInfectionStage target) {
        ticks = Math.max(ticks, (int) Math.ceil(target.startsAt * totalTicks) - 1);
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("Ticks", ticks);
        tag.putInt("Total", totalTicks);
        tag.putInt("Treatment", treatmentRate);
        tag.put("Site", site.save());
        return tag;
    }

    public static NeomorphInfection load(CompoundTag tag) {
        var infection = new NeomorphInfection(tag.getInt("Total"), InfectionSite.load(tag.getCompound("Site")));
        infection.ticks = tag.getInt("Ticks");
        infection.treatmentRate = Math.max(0, tag.getInt("Treatment"));
        infection.stage = NeomorphInfectionStage.forProgress(infection.progress());
        return infection;
    }
}
