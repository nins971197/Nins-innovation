package com.truenins.voiddomain.client;

import com.truenins.voiddomain.platform.Numbers;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;

final class VoidFeedback {
   private VoidFeedback() {
   }

   static void camera(ViewportEvent.ComputeCameraAngles event, UnlimitedVoid domain, long now, float partial, float effectScale) {
      float age = domain.age(now, partial);
      float strength = range(domain, event.getCamera().getPosition()) * domain.visibility(now, partial) * effectScale;
      if (!(strength < 0.001F)) {
         float invasion = 0.2F * smooth((age - 0.05F) / 0.2F) * (1.0F - smooth((age - 0.8F) / 0.4F));
         float breach = 0.74F * impulse(age - 1.2F, 0.65F) + 0.88F * impulse(age - 1.72F, 0.72F);
         float opened = 0.24F * impulse(age - 3.1F, 0.65F);
         float flow = (0.26F + 0.14F * gust(domain, age)) * smooth((age - 0.94F) / 0.6F);
         float amplitude = invasion + breach + opened + flow;
         float phase = (float)(domain.born & 1023L) * 0.17F;
         float pitch = amplitude * wave(age, 63.0F, phase) + breach * 0.16F * (float)Math.cos((double)(age * 24.0F)) + flow * 0.22F * (float)Math.sin((double)(age * 9.3F + phase));
         float yaw = amplitude * 0.56F * wave(age, 53.0F, phase + 2.1F) + flow * 0.14F * (float)Math.sin((double)(age * 7.7F + phase + 1.2F));
         float roll = amplitude * 0.63F * wave(age, 45.0F, phase + 4.3F) + flow * 0.18F * (float)Math.sin((double)(age * 8.6F + phase + 3.1F));
         event.setPitch(event.getPitch() + Numbers.clamp(pitch, -0.95F, 0.95F) * strength);
         event.setYaw(event.getYaw() + Numbers.clamp(yaw, -0.6F, 0.6F) * strength);
         event.setRoll(event.getRoll() + Numbers.clamp(roll, -0.65F, 0.65F) * strength);
      }
   }

   static float range(UnlimitedVoid domain, Vec3 listener) {
      float distance = (float)listener.distanceTo(domain.center);
      return 1.0F - smooth((distance - domain.radius) / Math.max(10.0F, domain.radius * 0.35F));
   }

   static float gust(UnlimitedVoid domain, float age) {
      float phase = (float)(domain.born & 1023L) * 0.17F;
      return 0.5F + 0.3F * (float)Math.sin((double)(age * 2.15F + phase)) + 0.2F * (float)Math.sin((double)(age * 5.37F + phase * 1.3F));
   }

   private static float impulse(float elapsed, float duration) {
      if (!(elapsed < 0.0F) && !(elapsed >= duration)) {
         float tail = 1.0F - elapsed / duration;
         return smooth(elapsed / 0.025F) * tail * tail;
      } else {
         return 0.0F;
      }
   }

   private static float wave(float age, float frequency, float phase) {
      return 0.68F * (float)Math.sin((double)(age * frequency + phase)) + 0.22F * (float)Math.sin((double)(age * frequency * 1.73F + phase * 1.9F)) + 0.1F * (float)Math.sin((double)(age * frequency * 2.71F + phase * 0.7F));
   }

   private static float smooth(float value) {
      value = Numbers.clamp(value, 0.0F, 1.0F);
      return value * value * (3.0F - 2.0F * value);
   }
}
