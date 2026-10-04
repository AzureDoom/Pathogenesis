package com.azure.pathogenesis.client.sound;

import com.azure.pathogenesis.registry.PathogenSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public class ContaminationLoopSound extends AbstractTickableSoundInstance {

    public ContaminationLoopSound() {
        super(PathogenSounds.CONTAMINATION_AMBIENT.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.pitch = ContaminationAmbience.pitch();
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    @Override
    public void tick() {
        var goal = ContaminationAmbience.targetIntensity() * 0.6F;
        volume += (goal - volume) * 0.04F;
        pitch += (ContaminationAmbience.pitch() - pitch) * 0.02F;
        if (goal <= 0.0F && volume < 0.01F) {
            stop();
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
