package com.truenins.mixin;

import com.mojang.blaze3d.shaders.BlendMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({BlendMode.class})
public interface EffectBlendModeAccessor {
   @Accessor("lastApplied")
   static BlendMode truenins$getLastApplied() {
      return BlendModeFallback.get();
   }

   @Accessor("lastApplied")
   static void truenins$setLastApplied(BlendMode blend) {
      BlendModeFallback.set(blend);
   }

   /**
    * Reflection path, used only when this accessor mixin was not applied.
    * The method bodies above are replaced by Mixin at runtime; if the mixin
    * is skipped (e.g. another mod also targets BlendMode and the accessor
    * fails to apply), the body runs instead of throwing, so domain rendering
    * degrades to "blend state not restored" instead of crashing the client.
    */
   final class BlendModeFallback {
      private static boolean lookedUp;
      private static java.lang.reflect.Field field;
      private static boolean warned;

      private BlendModeFallback() {
      }

      static synchronized BlendMode get() {
         java.lang.reflect.Field f = resolve();
         if (f == null) return null;
         try {
            return (BlendMode) f.get(null);
         } catch (ReflectiveOperationException e) {
            warn(e);
            return null;
         }
      }

      static synchronized void set(BlendMode blend) {
         java.lang.reflect.Field f = resolve();
         if (f == null) return;
         try {
            f.set(null, blend);
         } catch (ReflectiveOperationException e) {
            warn(e);
         }
      }

      private static java.lang.reflect.Field resolve() {
         if (lookedUp) return field;
         lookedUp = true;
         try {
            java.lang.reflect.Field f = BlendMode.class.getDeclaredField("lastApplied");
            f.setAccessible(true);
            field = f;
            warnOnce("Mixin accessor EffectBlendModeAccessor was not applied; "
                + "using reflection for BlendMode.lastApplied.");
         } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
         }
         return field;
      }

      private static void warn(Throwable t) {
         warnOnce("BlendMode.lastApplied is not reachable; blend state restore is skipped: " + t);
      }

      private static void warnOnce(String message) {
         if (warned) return;
         warned = true;
         org.slf4j.LoggerFactory.getLogger("truenins").warn("[truenins] {}", message);
      }
   }
}
