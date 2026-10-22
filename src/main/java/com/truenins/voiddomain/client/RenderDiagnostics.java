package com.truenins.voiddomain.client;

import com.mojang.logging.LogUtils;
import java.util.LinkedHashSet;
import java.util.Set;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

final class RenderDiagnostics {
   private static final Set<String> failures = new LinkedHashSet();
   private static boolean initialized;
   private static String renderer;
   private static String version;
   private static String lastFailure;

   private RenderDiagnostics() {
   }

   private static String string(int name) {
      String value = GL11.glGetString(name);
      return value == null ? "unknown" : value;
   }

   static void initialize() {
      if (!initialized) {
         renderer = string(7937);
         version = string(7938);
         initialized = true;
         LogUtils.getLogger().info("Jujutsu VFX graphics: original materials only; OS={}, vendor={}, renderer={}, GL={}, GLSL={}, max texture={}, framebuffer blit={}", new Object[]{System.getProperty("os.name", "unknown"), string(7936), renderer, version, string(35724), GL11.glGetInteger(3379), GL.getCapabilities().glBlitFramebuffer != 0L});
      }
   }

   static void fail(String reason) {
      initialize();
      lastFailure = reason;
      if (failures.size() < 16 && failures.add(reason)) {
         LogUtils.getLogger().warn("Jujutsu VFX render failure: {}. Only the failed pass is skipped; no simplified materials or automatic quality downgrade.", reason);
      }

   }

   static String diagnostic() {
      initialize();
      return "\u5b8c\u6574\u7279\u6548\uff08\u65e0\u7b80\u5316\u6a21\u5f0f\uff09 | " + renderer + " | " + version + (lastFailure == null ? "" : " | \u6700\u8fd1\u6e32\u67d3\u5f02\u5e38\uff1a" + lastFailure);
   }

   static void beginReload() {
      failures.clear();
      lastFailure = null;
      initialize();
   }
}
