package com.truenins.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

public class TNLoopingSound extends AbstractSoundInstance {

    public TNLoopingSound(ResourceLocation sound, float volume) {
        super(sound, SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.volume = volume;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }
}
