package com.truenins.voiddomain.core;

public final class DomainDuration {
   public static final int DEFAULT_SECONDS = 5;
   public static final int MIN_SECONDS = 5;
   public static final int MAX_SECONDS = 120;

   private DomainDuration() {
   }

   public static boolean valid(int seconds) {
      return seconds >= 5 && seconds <= 120;
   }

   public static int ticks(int seconds) {
      if (!valid(seconds)) {
         throw new IllegalArgumentException("Domain duration must be 5-120 seconds");
      } else {
         return seconds * 20;
      }
   }

   public static int castTicks(int configuredSeconds, int requestedTicks, boolean scripted) {
      return scripted ? requestedTicks : ticks(configuredSeconds);
   }
}
