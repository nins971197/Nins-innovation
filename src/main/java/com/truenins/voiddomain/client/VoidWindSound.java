package com.truenins.voiddomain.client;

import com.truenins.voiddomain.platform.Numbers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

final class VoidWindSound extends AbstractTickableSoundInstance {
   private static final SoundEvent AIR = com.truenins.register.TNSounds.VOID_AIR_FRICTION.get();
   private final UnlimitedVoid domain;
   private final ClientLevel owner;

   VoidWindSound(UnlimitedVoid domain, ClientLevel owner) {
      super(AIR, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
      this.domain = domain;
      this.owner = owner;
      this.looping = true;
      this.delay = 0;
      this.relative = true;
      this.attenuation = Attenuation.NONE;
      this.volume = 0.0F;
      this.pitch = 1.0F;
   }

   public boolean canStartSilent() {
      return true;
   }

   public boolean canPlaySound() {
      return !this.isStopped();
   }

   public void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == this.owner && mc.player != null && mc.player.isAlive() && !this.domain.expired(ClientEffectClock.ticks())) {
         long now = ClientEffectClock.ticks();
         float age = this.domain.age(now, 0.0F);
         float reveal = Numbers.clamp((age - 0.94F) / 0.8F, 0.0F, 1.0F);
         reveal = reveal * reveal * (3.0F - 2.0F * reveal);
         float gust = VoidFeedback.gust(this.domain, age);
         float target = (0.5F + 0.16F * gust) * reveal * this.domain.visibility(now, 0.0F) * VoidFeedback.range(this.domain, mc.gameRenderer.getMainCamera().getPosition());
         this.volume += (target - this.volume) * 0.32F;
         this.pitch = 1.0F + 0.07F * gust;
      } else {
         this.stop();
      }
   }

   void cancel() {
      this.stop();
      Minecraft.getInstance().getSoundManager().stop(this);
   }
}
