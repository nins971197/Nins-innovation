package com.truenins.client;

import com.truenins.TrueNinsMod;
import com.truenins.client.particle.TNFlashParticle;
import com.truenins.client.particle.TNMoteParticle;
import com.truenins.register.TNParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TNClientParticles {

    private TNClientParticles() {}

    @SubscribeEvent
    public static void onRegisterProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(TNParticles.MOTE_SHARD.get(),
            sprites -> new TNMoteParticle.Provider(sprites, TNMoteParticle.LOOK_SHARD));
        event.registerSpriteSet(TNParticles.MOTE_SPARK.get(),
            sprites -> new TNMoteParticle.Provider(sprites, TNMoteParticle.LOOK_SPARK));
        event.registerSpriteSet(TNParticles.MOTE_VEIL.get(),
            sprites -> new TNMoteParticle.Provider(sprites, TNMoteParticle.LOOK_VEIL));
        event.registerSpriteSet(TNParticles.MOTE_STAR.get(),
            sprites -> new TNMoteParticle.Provider(sprites, TNMoteParticle.LOOK_STAR));
        event.registerSpriteSet(TNParticles.MOTE_CROSS.get(),
            sprites -> new TNFlashParticle.Provider(sprites, TNFlashParticle.LOOK_CROSS));
        event.registerSpriteSet(TNParticles.MOTE_CROSS_SMALL.get(),
            sprites -> new TNFlashParticle.Provider(sprites, TNFlashParticle.LOOK_CROSS));
        event.registerSpriteSet(TNParticles.FLASH_HALO.get(),
            sprites -> new TNFlashParticle.Provider(sprites, TNFlashParticle.LOOK_HALO));
        event.registerSpriteSet(TNParticles.FLASH_SHARD.get(),
            sprites -> new TNFlashParticle.Provider(sprites, TNFlashParticle.LOOK_SHARD));
        event.registerSpriteSet(TNParticles.FLASH_SLASH.get(),
            sprites -> new TNFlashParticle.Provider(sprites, TNFlashParticle.LOOK_SLASH));
        event.registerSpriteSet(TNParticles.MOTE_TIDE.get(),
            sprites -> new TNMoteParticle.Provider(sprites, TNMoteParticle.LOOK_TIDE));
        event.registerSpriteSet(TNParticles.MOTE_EMBER.get(),
            sprites -> new TNMoteParticle.Provider(sprites, TNMoteParticle.LOOK_EMBER));
    }
}
