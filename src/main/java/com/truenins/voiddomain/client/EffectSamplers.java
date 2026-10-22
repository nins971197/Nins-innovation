package com.truenins.voiddomain.client;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

final class EffectSamplers implements AutoCloseable {
   private static final int UNITS = 4;
   private static int nesting;
   private final int[] previous;
   private boolean closed;

   EffectSamplers() {
      if (nesting++ == 0 && GL.getCapabilities().glBindSampler != 0L && GL.getCapabilities().glGetIntegeri_v != 0L) {
         this.previous = new int[4];

         for(int unit = 0; unit < 4; ++unit) {
            this.previous[unit] = GL30.glGetIntegeri(35097, unit);
            if (this.previous[unit] != 0) {
               GL33.glBindSampler(unit, 0);
            }
         }

      } else {
         this.previous = null;
      }
   }

   public void close() {
      if (!this.closed) {
         this.closed = true;
         --nesting;
         if (this.previous != null) {
            for(int unit = 0; unit < 4; ++unit) {
               if (this.previous[unit] != 0) {
                  GL33.glBindSampler(unit, this.previous[unit]);
               }
            }
         }

      }
   }
}
