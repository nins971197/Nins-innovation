package com.truenins.voiddomain.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.truenins.voiddomain.platform.Numbers;
import com.truenins.voiddomain.platform.RenderBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

final class VoidStreamRenderer {
   private static ShaderInstance shader;
   private static final int[] PALETTE = new int[]{13708631, 5315016, 15309303};

   VoidStreamRenderer() {
   }

   static void setShader(ShaderInstance value) {
      shader = value;
   }

   static void render(RenderLevelStageEvent event, UnlimitedVoid domain) {
      Minecraft mc = Minecraft.getInstance();
      if (shader != null && mc.level != null) {
         long tick = ClientEffectClock.ticks();
         float partial = event.getPartialTick();
         float age = domain.age(tick, partial);
         if (age >= UnlimitedVoid.HOLE_TIME) return;
         float strength = domain.visibility(tick, partial) * smooth((age - 0.86F) / 0.32F) * (1.0F - smooth((age - 2.60F) / 0.80F));
         if (!(strength < 0.001F)) {
            float radius = domain.radius;
            Vec3 camera = event.getCamera().getPosition();
            Vec3 direction = domain.axis;
            if (!(camera.distanceToSqr(domain.center) > (double)((radius + 160.0F) * (radius + 160.0F)))) {
               Vec3 right = (new Vec3(0.0D, 1.0D, 0.0D)).cross(direction).normalize();
               Vec3 up = direction.cross(right).normalize();
               int count = Numbers.clamp((long)((int)((double)(radius * radius) * 2.4D)), 600, 2600);

               try (GlState.State ignored = new GlState.State()) {
                  shader.safeGetUniform("ViewProjection").set((new Matrix4f(event.getProjectionMatrix())).mul(event.getPoseStack().last().pose()));
                  RenderSystem.setShader(() -> shader);
                  RenderSystem.enableDepthTest();
                  RenderSystem.depthFunc(515);
                  RenderSystem.depthMask(false);
                  RenderSystem.disableCull();
                  RenderSystem.enableBlend();
                  RenderSystem.blendFunc(770, 1);
                  BufferBuilder b = RenderBuffers.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

                  for(int i = 0; i < count; ++i) {
                     double seed = hash((double)i * 11.7D + 2.8D);
                     double angle = hash((double)i * 5.17D + 9.1D) * 3.141592653589793D * 2.0D;
                     double across = (double)radius * Math.sqrt(hash((double)i * 17.3D + 3.7D)) * 0.94D;
                     double reach = Math.sqrt((double)(radius * radius) - across * across);
                     double speed = 96.0D + seed * 80.0D;
                     double phase = fract((double)age * speed / (reach * 2.0D) + seed * 17.0D);
                     double along = (phase * 2.0D - 1.0D) * reach;
                     Vec3 point = domain.center.add(right.scale(Math.cos(angle) * across)).add(up.scale(Math.sin(angle) * across)).add(direction.scale(along));
                     if (!(point.y < domain.center.y + 0.1D)) {
                        float length = (float)Math.min(7.0D + seed * 14.0D, Math.max(0.0D, 2.0D * (reach - Math.abs(along))));
                        float opacity = (float)(Math.min(phase * 12.0D, 1.0D) * Math.min((1.0D - phase) * 12.0D, 1.0D)) * strength * (0.52F + (float)seed * 0.44F);
                        if (!((double)length < 0.08D) && !(opacity < 0.015F)) {
                           Vec3 facing = camera.subtract(point).normalize();
                           Vec3 side = direction.cross(facing);
                           if (side.lengthSqr() < 1.0E-4D) {
                              side = right;
                           }

                           side = side.normalize().scale(0.098D + seed * 0.150D);
                           Vec3 half = direction.scale((double)length * 0.5D);
                           int rgb = PALETTE[i % PALETTE.length];
                           int color = Numbers.clamp((long)Math.round(opacity * 255.0F), 0, 255) << 24 | rgb;
                           vertex(b, point.subtract(side).subtract(half).subtract(camera), -1.0F, -1.0F, color);
                           vertex(b, point.add(side).subtract(half).subtract(camera), 1.0F, -1.0F, color);
                           vertex(b, point.add(side).add(half).subtract(camera), 1.0F, 1.0F, color);
                           vertex(b, point.subtract(side).add(half).subtract(camera), -1.0F, 1.0F, color);
                        }
                     }
                  }

                  BufferBuilder.RenderedBuffer mesh = b.endOrDiscardIfEmpty();
                  if (mesh != null) {
                     BufferUploader.drawWithShader(mesh);
                  }
               }

            }
         }
      }
   }

   private static void vertex(BufferBuilder b, Vec3 p, float u, float v, int color) {
      b.vertex((double)((float)p.x), (double)((float)p.y), (double)((float)p.z)).uv(u, v).color(color).endVertex();
   }

   private static float smooth(float value) {
      value = Numbers.clamp(value, 0.0F, 1.0F);
      return value * value * (3.0F - 2.0F * value);
   }

   private static double hash(double n) {
      return fract(Math.sin(n * 12.9898D) * 43758.5453D);
   }

   private static double fract(double n) {
      return n - Math.floor(n);
   }
}
