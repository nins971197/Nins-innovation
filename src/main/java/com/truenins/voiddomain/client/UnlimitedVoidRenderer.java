package com.truenins.voiddomain.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.truenins.voiddomain.platform.RenderBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

final class UnlimitedVoidRenderer {
   private static ShaderInstance shader;
   private static long lastLogged = -1L;

   UnlimitedVoidRenderer() {
   }

   private static String round(double value) {
      return String.format(java.util.Locale.ROOT, "%.2f", value);
   }

   static void setShader(ShaderInstance value) {
      shader = value;
   }

   static boolean ready() {
      return shader != null;
   }

   static void render(RenderLevelStageEvent event, UnlimitedVoid domain) {
      Minecraft mc = Minecraft.getInstance();
      if (shader != null && mc.level != null) {
         long tick = ClientEffectClock.ticks();
         float partial = event.getPartialTick();
         float radius = domain.radius * domain.expansion(tick, partial);
         float visibility = domain.visibility(tick, partial);
         Vec3 camera = event.getCamera().getPosition();
         if (!(visibility < 0.001F)) {
            boolean interior = VoidDomain.interior(domain, camera, tick, partial);
            GlState.State ignored = new GlState.State();

            try {
               boolean sceneReady = WaterSceneCapture.capture();
               DomainWorldCapture.bind(shader, true);
               if (sceneReady) {
                  shader.setSampler("SceneColor", WaterSceneCapture.color());
                  shader.setSampler("SceneDepth", WaterSceneCapture.depth());
               }

               Matrix4f projection = (new Matrix4f(event.getProjectionMatrix())).mul(event.getPoseStack().last().pose());
               shader.safeGetUniform("ViewProjection").set(projection);
               shader.safeGetUniform("InverseViewProjection").set((new Matrix4f(projection)).invert());
               Matrix4f skyView = (new Matrix4f(event.getPoseStack().last().pose())).setTranslation(0.0F, 0.0F, 0.0F);
               shader.safeGetUniform("SkyInverseViewProjection").set((new Matrix4f(event.getProjectionMatrix())).mul(skyView).invert());
               Vec3 center = domain.center.subtract(camera);
               shader.safeGetUniform("CenterRelative").set((float)center.x, (float)center.y, (float)center.z);
               shader.safeGetUniform("Axis").set((float)domain.axis.x, (float)domain.axis.y, (float)domain.axis.z);
               shader.safeGetUniform("Radius").set(Math.max(radius, 0.001F));
               shader.safeGetUniform("InteriorView").set(interior ? 1.0F : 0.0F);
               shader.safeGetUniform("BreakProgress").set(0.0F);
               shader.safeGetUniform("BreakStyle").set(0.0F);
               shader.safeGetUniform("OuterCenter").set(0.0F, 0.0F, 0.0F);
               shader.safeGetUniform("OuterRadius").set(0.0F);
               shader.safeGetUniform("Time").set(domain.age(tick, partial));
               shader.safeGetUniform("HoleMask").set(
                  domain.age(tick, partial) >= UnlimitedVoid.HOLE_TIME ? 1.0F : 0.0F);
               shader.safeGetUniform("Visibility").set(visibility);
               shader.safeGetUniform("Enclosed").set(domain.enclosed ? 1.0F : 0.0F);
               shader.safeGetUniform("UseScene").set(sceneReady ? 1.0F : 0.0F);
               shader.safeGetUniform("ScreenEffects").set(((Double)mc.options.screenEffectScale().get()).floatValue());
               shader.safeGetUniform("FlashEnabled").set(1.0F);
               if (tick % 20 == 0L && tick != lastLogged) {
                  lastLogged = tick;
                  com.truenins.TrueNinsMod.LOGGER.info(
                     "[void] t={} age={} vis={} r={} enclosed={} interior={} scene={} cap={}"
                        + " centerRel=({},{},{}) axis=({},{},{}) cam=({},{},{})",
                     tick, round(domain.age(tick, partial)), round(visibility), round(radius),
                     domain.enclosed, interior, sceneReady, DomainWorldCapture.status(),
                     round(center.x), round(center.y), round(center.z),
                     round(domain.axis.x), round(domain.axis.y), round(domain.axis.z),
                     round(camera.x), round(camera.y), round(camera.z));
               }

               RenderSystem.setShader(() -> shader);
               RenderSystem.enableDepthTest();               RenderSystem.depthFunc(sceneReady ? 519 : 515);
               RenderSystem.depthMask(!interior);
               RenderSystem.disableCull();
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               BufferBuilder b = RenderBuffers.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
               b.vertex(-1.0D, -1.0D, 1.0D).uv(0.0F, 0.0F).color(-1).endVertex();
               b.vertex(1.0D, -1.0D, 1.0D).uv(1.0F, 0.0F).color(-1).endVertex();
               b.vertex(1.0D, 1.0D, 1.0D).uv(1.0F, 1.0F).color(-1).endVertex();
               b.vertex(-1.0D, 1.0D, 1.0D).uv(0.0F, 1.0F).color(-1).endVertex();
               BufferUploader.drawWithShader(b.end());
            } catch (Throwable var19) {
               try {
                  ignored.close();
               } catch (Throwable var18) {
                  var19.addSuppressed(var18);
               }

               throw var19;
            }

            ignored.close();
            if (interior) {
               VoidStreamRenderer.render(event, domain);
            }

         }
      }
   }
}
