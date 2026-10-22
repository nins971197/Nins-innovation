package com.truenins.voiddomain.core;

public final class EffectClock {
   private Object world;
   private long ticks;

   public EffectClock() {
   }

   public void changeWorld(Object next) {
      if (this.world != next) {
         this.world = next;
         this.ticks = 0L;
      }

   }

   public void tick(Object currentWorld, boolean paused) {
      this.changeWorld(currentWorld);
      if (this.world != null && !paused) {
         ++this.ticks;
      }

   }

   public long ticks() {
      return this.ticks;
   }

   public long after(int remainingTicks) {
      return this.ticks + (long)Math.max(0, remainingTicks);
   }
}
