package com.truenins.voiddomain.client;

public final class WaterSceneCapture {
   private static final SceneSnapshot SCENE = new SceneSnapshot();

   private WaterSceneCapture() {
   }

   public static int color() {
      return SCENE.color();
   }

   public static int depth() {
      return SCENE.depth();
   }

   public static boolean available() {
      return SCENE.available();
   }

   public static boolean capture() {
      return SCENE.capture();
   }

   /** Seeds an offscreen target with the captured colour so a translucent first pass blends
    *  over this frame instead of whatever the target held from the previous frame. */
   public static boolean seed(com.mojang.blaze3d.pipeline.TextureTarget destination) {
      return SCENE.blitColorTo(destination);
   }


   public static void release() {
      SCENE.release();
   }
}
