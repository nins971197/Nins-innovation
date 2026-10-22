package com.truenins.voiddomain.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

final class DomainWorldCapture {
   private static final SceneSnapshot TERRAIN = new SceneSnapshot();
   private static final SceneSnapshot ENTITIES = new SceneSnapshot();
   private static ShaderInstance clearShader;
   private static boolean wanted;
   private static boolean ready;
   private static boolean cleared;
   private static Object level;

   private DomainWorldCapture() {
   }

   static void setShader(ShaderInstance shader) {
      release();
      clearShader = shader;
   }

   static void release() {
      TERRAIN.release();
      ENTITIES.release();
      cleared = false;
      ready = false;
      wanted = false;
      level = null;
   }

   static String status() {
      return "w" + (wanted ? 1 : 0) + "/r" + (ready ? 1 : 0) + "/c" + (cleared ? 1 : 0)
         + "/s" + (clearShader != null ? 1 : 0);
   }

   static void beginFrame() {
      Minecraft mc = Minecraft.getInstance();
      if (level != mc.level) {
         release();
         level = mc.level;
      }

      cleared = false;
      ready = false;
      wanted = mc.level != null && VoidDomain.active() != null;
      if (!wanted) {
         TERRAIN.release();
         ENTITIES.release();
      }

   }

   static void terrain(RenderLevelStageEvent event) {
      if (wanted && clearShader != null) {
         UnlimitedVoid domain = VoidDomain.active();
         if (domain != null) {
            long tick = ClientEffectClock.ticks();
            float partial = event.getPartialTick();
            Vec3 camera = event.getCamera().getPosition();
            if (!(domain.age(tick, partial) < 1.0F) && !(domain.visibility(tick, partial) < 0.99F) && VoidDomain.interior(domain, camera, tick, partial)) {
               try (GlState.State state = new GlState.State()) {
                  try (SceneSnapshot.Target target = new SceneSnapshot.Target()) {
                     if (TERRAIN.capture()) {
                        Matrix4f matrix = (new Matrix4f(event.getProjectionMatrix())).mul(event.getPoseStack().last().pose());
                        clearShader.setSampler("SceneDepth", TERRAIN.depth());
                        clearShader.safeGetUniform("InverseViewProjection").set(matrix.invert());
                        Vec3 center = domain.center.subtract(camera);
                        clearShader.safeGetUniform("CenterRelative").set((float)center.x, (float)center.y, (float)center.z);
                        clearShader.safeGetUniform("Radius").set(domain.radius * domain.expansion(tick, partial));
                        RenderSystem.setShader(() -> clearShader);
                        RenderSystem.enableDepthTest();
                        RenderSystem.depthFunc(519);
                        RenderSystem.depthMask(true);
                        RenderSystem.disableBlend();
                        RenderSystem.disableCull();
                        quad();
                        cleared = true;
                     }
                  }
               }
            }
         }
      }
   }

   static void entities() {
      if (wanted) {
         Minecraft.getInstance().renderBuffers().bufferSource().endBatch();

         try (
            GlState.State state = new GlState.State();
            SceneSnapshot.Target target = new SceneSnapshot.Target();
         ) {
            ready = ENTITIES.capture();
         }

      }
   }

   static void bind(ShaderInstance shader, boolean requireCleared) {
      boolean use = ready && (!requireCleared || cleared);
      shader.safeGetUniform("RawSceneReady").set(use ? 1.0F : 0.0F);
      shader.setSampler("RawSceneColor", use ? ENTITIES.color() : 0);
      shader.setSampler("RawSceneDepth", use ? ENTITIES.depth() : 0);
   }

   private static void quad() {
      BufferBuilder b = Tesselator.getInstance().getBuilder();
      b.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      b.vertex(-1.0D, -1.0D, 1.0D).uv(0.0F, 0.0F).color(-1).endVertex();
      b.vertex(1.0D, -1.0D, 1.0D).uv(1.0F, 0.0F).color(-1).endVertex();
      b.vertex(1.0D, 1.0D, 1.0D).uv(1.0F, 1.0F).color(-1).endVertex();
      b.vertex(-1.0D, 1.0D, 1.0D).uv(0.0F, 1.0F).color(-1).endVertex();
      BufferUploader.drawWithShader(b.end());
   }
}
