package com.azure.pathogenesis.client.sound;

import com.azure.pathogenesis.registry.PathogenSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public class ContaminationLoopSound extends AbstractTickableSoundInstance {

    private static final float MAX_VOLUME = 0.6F;

    private static final float FADE = 0.04F;

    public ContaminationLoopSound() {
        super(PathogenSounds.CONTAMINATION_AMBIENT.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    @Override
    public void tick() {
        var goal = ContaminationAmbience.intensity() * MAX_VOLUME;
        volume += (goal - volume) * FADE;
        if (goal <= 0.0F && volume < 0.01F) {
            stop();
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
