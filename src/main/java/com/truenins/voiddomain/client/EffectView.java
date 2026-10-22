package com.truenins.voiddomain.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

final class EffectView {
   private EffectView() {
   }

   static double distanceToSqr(Vec3 point) {
      Minecraft mc = Minecraft.getInstance();
      return mc.level != null && mc.player != null ? mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(point) : 1.0D / 0.0D;
   }
}
