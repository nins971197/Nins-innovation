package com.truenins.voiddomain.client;

import com.truenins.voiddomain.core.DomainVolume;
import com.truenins.voiddomain.platform.Numbers;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

final class UnlimitedVoid {
   static final float HOLE_TIME = 4.70F;
   static final float HOLE_SNAP = 0.22F;
   final Vec3 center;
   final Vec3 axis;
   final float radius;
   final long born;
   private long settlesAt;
   private long ends = 9223372036854775807L;
   private long nextWindCheck;
   private VoidWindSound wind;
   private boolean opened;
   private boolean struck;
   private boolean ending;
   boolean enclosed = true;

   UnlimitedVoid(Minecraft mc, float radius, float seconds) {
      this(mc, mc.player.position().add(0.0D, 0.025D, 0.0D), mc.player.getLookAngle(), radius, ClientEffectClock.ticks(), Math.round(seconds * 20.0F));
   }

   UnlimitedVoid(Minecraft mc, Vec3 center, Vec3 look, float radius, long born, int remaining) {
      this.radius = radius;
      this.center = center;
      Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
      if (horizontal.lengthSqr() < 0.001D) {
         horizontal = new Vec3(0.0D, 0.0D, 1.0D);
      }

      this.axis = horizontal.normalize().add(0.0D, 0.24D, 0.0D).normalize();
      this.born = born;
      this.settlesAt = ClientEffectClock.ticks() + (long)remaining;
      this.opened = this.age(ClientEffectClock.ticks(), 0.0F) >= 0.94F;
      if (ClientEffectClock.ticks() - born < 4L) {
         this.sound(mc, SoundEvents.BEACON_ACTIVATE, 0.5F, 1.55F);
      }

   }

   float age(long tick, float partial) {
      return ((float)(tick - this.born) + partial) / 20.0F;
   }

   float remaining(long tick) {
      return Math.max(0.0F, (float)(this.settlesAt - tick) / 20.0F);
   }

   float expansion(long tick, float partial) {
      return DomainVolume.expansion(2, Math.min((float)(tick - this.born) + partial, (float)(this.settlesAt - this.born)) / 20.0F);
   }

   float visibility(long tick, float partial) {
      return smooth(this.age(tick, partial) / 0.2F) * (this.ends == 9223372036854775807L ? 1.0F : smooth(((float)(this.ends - tick) - partial) / 26.0F));
   }

   boolean expired(long tick) {
      return tick >= this.ends;
   }

   void awaitSettlement(long tick) {
      this.settlesAt = Math.min(this.settlesAt, tick);
   }

   void reschedule(long deadline) {
      if (this.ends == 9223372036854775807L) {
         this.settlesAt = deadline;
      }

   }

   void close(long tick) {
      this.awaitSettlement(tick);
      this.ends = Math.min(this.ends, tick + 26L);
   }

   String phase(long tick) {
      if (this.ends != 9223372036854775807L) {
         return "\u6d88\u9000 \u00b7 \u5373\u5c06\u89e3\u9664\u51bb\u7ed3";
      } else if (tick >= this.settlesAt) {
         return "\u7b49\u5f85\u670d\u52a1\u7aef\u7ed3\u675f\u786e\u8ba4";
      } else {
         return this.age(tick, 0.0F) < 0.94F ? "\u767d\u8272\u4fb5\u67d3 \u00b7 \u65f6\u95f4\u9759\u6b62" : (this.age(tick, 0.0F) < 3.1F ? "\u767d\u5e55\u7834\u5f00" : "\u65e0\u91cf\u7a7a\u5904 \u00b7 \u65f6\u95f4\u9759\u6b62");
      }
   }

   void tick(Minecraft mc) {
      long now = ClientEffectClock.ticks();
      if (!this.opened && this.age(now, 0.0F) >= 0.94F) {
         this.opened = true;
         this.sound(mc, SoundEvents.AMETHYST_BLOCK_CHIME, 0.85F, 0.65F);
      }

      if (!this.struck && this.age(now, 0.0F) >= HOLE_TIME) {
         this.struck = true;
         this.sound(mc, com.truenins.register.TNSounds.VOID_DONG.get(), 2.4F, 1.0F);
      }

      if (this.opened && !this.expired(now) && mc.player != null && mc.player.isAlive() && now >= this.nextWindCheck) {
         this.nextWindCheck = now + 20L;
         if (this.wind == null || !mc.getSoundManager().isActive(this.wind)) {
            if (this.wind != null) {
               this.wind.cancel();
            }

            this.wind = new VoidWindSound(this, mc.level);
            mc.getSoundManager().play(this.wind);
         }
      }

      if (!this.ending && this.ends != 9223372036854775807L) {
         this.ending = true;
         this.sound(mc, SoundEvents.BEACON_DEACTIVATE, 0.45F, 1.1F);
      }

   }

   void stop() {
      if (this.wind != null) {
         this.wind.cancel();
      }

      this.wind = null;
   }

   private void sound(Minecraft mc, SoundEvent sound, float volume, float pitch) {
      mc.level.playLocalSound(this.center.x, this.center.y + 1.0D, this.center.z, sound, SoundSource.BLOCKS, volume, pitch, false);
   }

   private static float smooth(float t) {
      t = Numbers.clamp(t, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }
}
