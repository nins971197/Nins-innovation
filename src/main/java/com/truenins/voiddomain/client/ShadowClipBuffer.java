package com.truenins.voiddomain.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Vector4f;

final class ShadowClipBuffer {
   private ShadowClipBuffer() {
   }

   static MultiBufferSource above(MultiBufferSource source, Matrix4f entityPose) {
      Vector4f plane = (new Matrix4f(entityPose)).invert().transpose().transform(new Vector4f(0.0F, 1.0F, 0.0F, -0.09F));
      return (type) -> new ShadowClipBuffer.Clip(source.getBuffer(type), plane);
   }

   static List<float[]> clip(float[][] quad, Vector4f plane) {
      List<float[]> out = new ArrayList(6);
      float[] a = quad[3];
      float da = distance(a, plane);

      for(float[] b : quad) {
         float db = distance(b, plane);
         if (da >= 0.0F != db >= 0.0F) {
            float t = da / (da - db);
            float[] p = new float[16];

            for(int i = 0; i < 16; ++i) {
               p[i] = a[i] + (b[i] - a[i]) * t;
            }

            out.add(p);
         }

         if (db >= 0.0F) {
            out.add(b);
         }

         a = b;
         da = db;
      }

      return out;
   }

   private static float distance(float[] v, Vector4f p) {
      return v[0] * p.x + v[1] * p.y + v[2] * p.z + p.w;
   }

   private static final class Clip implements VertexConsumer {
      final VertexConsumer output;
      final Vector4f plane;
      final float[][] quad = new float[4][];
      float[] vertex = new float[16];
      int count;
      int dr = 255;
      int dg = 255;
      int db = 255;
      int da = 255;

      Clip(VertexConsumer output, Vector4f plane) {
         this.output = output;
         this.plane = plane;
      }

      private void begin(float x, float y, float z) {
         this.vertex = new float[]{x, y, z, (float)this.dr, (float)this.dg, (float)this.db, (float)this.da, 0.0F, 0.0F, 0.0F, 10.0F, 240.0F, 240.0F, 0.0F, 1.0F, 0.0F};
      }

      private void end() {
         this.quad[this.count++] = this.vertex;
         if (this.count >= 4) {
            this.count = 0;
            boolean full = true;
            boolean empty = true;

            for(float[] v : this.quad) {
               boolean in = ShadowClipBuffer.distance(v, this.plane) >= 0.0F;
               full &= in;
               empty &= !in;
            }

            if (!empty) {
               if (full) {
                  for(float[] v : this.quad) {
                     this.emit(v);
                  }

               } else {
                  List<float[]> cut = ShadowClipBuffer.clip(this.quad, this.plane);

                  for(int i = 1; i + 1 < cut.size(); ++i) {
                     this.emit((float[])cut.get(0));
                     this.emit((float[])cut.get(i));
                     this.emit((float[])cut.get(i + 1));
                     this.emit((float[])cut.get(i + 1));
                  }

               }
            }
         }
      }

      public VertexConsumer vertex(double x, double y, double z) {
         this.begin((float)x, (float)y, (float)z);
         return this;
      }

      public VertexConsumer color(int r, int g, int b, int a) {
         this.vertex[3] = (float)r;
         this.vertex[4] = (float)g;
         this.vertex[5] = (float)b;
         this.vertex[6] = (float)a;
         return this;
      }

      public VertexConsumer uv(float u, float v) {
         this.vertex[7] = u;
         this.vertex[8] = v;
         return this;
      }

      public VertexConsumer overlayCoords(int u, int v) {
         this.vertex[9] = (float)u;
         this.vertex[10] = (float)v;
         return this;
      }

      public VertexConsumer uv2(int u, int v) {
         this.vertex[11] = (float)u;
         this.vertex[12] = (float)v;
         return this;
      }

      public VertexConsumer normal(float x, float y, float z) {
         this.vertex[13] = x;
         this.vertex[14] = y;
         this.vertex[15] = z;
         return this;
      }

      public void endVertex() {
         this.end();
      }

      public void defaultColor(int r, int g, int b, int a) {
         this.dr = r;
         this.dg = g;
         this.db = b;
         this.da = a;
      }

      public void unsetDefaultColor() {
         this.dr = this.dg = this.db = this.da = 255;
      }

      private void emit(float[] v) {
         this.output.vertex((double)v[0], (double)v[1], (double)v[2]).color(Math.round(v[3]), Math.round(v[4]), Math.round(v[5]), Math.round(v[6])).uv(v[7], v[8]).overlayCoords(Math.round(v[9]), Math.round(v[10])).uv2(Math.round(v[11]), Math.round(v[12])).normal(v[13], v[14], v[15]).endVertex();
      }
   }
}
