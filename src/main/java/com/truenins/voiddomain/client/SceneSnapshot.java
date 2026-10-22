package com.truenins.voiddomain.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

final class SceneSnapshot {
   static int lastWidth;
   static int lastHeight;

   private TextureTarget scene;
   private int depthFormat;
   private boolean available;

   SceneSnapshot() {
   }

   int color() {
      return this.scene == null ? 0 : this.scene.getColorTextureId();
   }

   int depth() {
      return this.scene == null ? 0 : this.scene.getDepthTextureId();
   }


   boolean available() {
      return this.available;
   }

   void release() {
      if (this.scene != null) {
         this.scene.destroyBuffers();
         this.scene = null;
      }

      this.available = false;
   }

   void bind() {
      if (!this.available) {
         throw new IllegalStateException("Scene snapshot is not ready");
      } else {
         GL30.glBindFramebuffer(36160, this.scene.frameBufferId);
         RenderSystem.viewport(0, 0, this.scene.width, this.scene.height);
      }
   }

   boolean copyFrom(SceneSnapshot source) {
      if (!source.available()) {
         return false;
      } else {
         try (SceneSnapshot.Target ignored = new SceneSnapshot.Target()) {
            source.bind();
            return this.capture();
         }
      }
   }

   /** Copies the captured colour (no depth) into another framebuffer. Used to seed a
    *  composition chain with the frame it is about to be blended over. */
   boolean blitColorTo(TextureTarget destination) {
      if (!this.available || this.scene == null || destination == null) {
         return false;
      } else {
         int read = GL11.glGetInteger(36010);
         int draw = GL11.glGetInteger(36006);
         int[] viewport = new int[4];
         GL11.glGetIntegerv(2978, viewport);
         try {
            GL30.glBindFramebuffer(36008, this.scene.frameBufferId);
            GL30.glBindFramebuffer(36009, destination.frameBufferId);
            GL11.glDisable(3089);
            GL30.glBlitFramebuffer(0, 0, this.scene.width, this.scene.height,
                0, 0, destination.width, destination.height, 16384, 9728);
            RenderSystem.viewport(0, 0, destination.width, destination.height);
            return GL11.glGetError() == 0;
         } catch (RuntimeException e) {
            RenderDiagnostics.fail("chain seed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
         } finally {
            GL30.glBindFramebuffer(36008, read);
            GL30.glBindFramebuffer(36009, draw);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
         }
      }
   }

   boolean capture() {
      return this.capture(true);
   }

   boolean captureColor() {
      return this.capture(false);
   }

   private boolean capture(boolean requireDepth) {
      this.available = false;
      RenderDiagnostics.initialize();
      int draw = GL11.glGetInteger(36006);
      int read = GL11.glGetInteger(36010);
      int active = GL11.glGetInteger(34016);
      int texture = GL11.glGetInteger(32873);
      int[] viewport = new int[4];
      GL11.glGetIntegerv(2978, viewport);
      float[] clear = new float[4];
      GL11.glGetFloatv(3106, clear);
      boolean scissor = GL11.glIsEnabled(3089);
      int sourceReadBuffer = -1;
      if (viewport[2] >= 1 && viewport[3] >= 1) {
         if (draw == 0) {
            RenderDiagnostics.fail("scene capture has no offscreen world framebuffer");
            return false;
         } else {
            int maximum = GL11.glGetInteger(3379);
            if (viewport[2] <= maximum && viewport[3] <= maximum) {
               int format;
               try {
                  GL30.glBindFramebuffer(36008, draw);
                  sourceReadBuffer = GL11.glGetInteger(3074);
                  int drawBuffer = GL11.glGetInteger(34853);
                  if (drawBuffer != 0) {
                     GL11.glReadBuffer(drawBuffer);
                     format = 0;
                     int type = 0;
                     int pixel = 0;
                     if (requireDepth) {
                        int object = GL30.glGetFramebufferAttachmentParameteri(36008, 36096, 36048);
                        if (object == 0) {
                           return false;
                        }

                        int bits = GL30.glGetFramebufferAttachmentParameteri(36008, 36096, 33302);
                        int stencil = GL30.glGetFramebufferAttachmentParameteri(36008, 36096, 33303);
                        int component = GL30.glGetFramebufferAttachmentParameteri(36008, 36096, 33297);
                        if (stencil > 0) {
                           format = component == 5126 ? '\u8cad' : '\u88f0';
                           type = component == 5126 ? '\u8dad' : '\u84fa';
                           pixel = 34041;
                        } else {
                           format = component == 5126 ? '\u8cac' : (bits == 32 ? '\u81a7' : (bits == 24 ? '\u81a6' : '\u81a5'));
                           type = component == 5126 ? 5126 : 5125;
                           pixel = 6402;
                        }
                     }

                     GL11.glDisable(3089);
                     if (this.scene == null || this.scene.width != viewport[2] || this.scene.height != viewport[3] || this.depthFormat != format) {
                        this.release();
                        this.scene = new EffectTarget(viewport[2], viewport[3], requireDepth, Minecraft.ON_OSX);
                        lastWidth = viewport[2];
                        lastHeight = viewport[3];
                        this.depthFormat = format;
                        if (requireDepth) {
                           RenderSystem.bindTexture(this.scene.getDepthTextureId());
                           GL11.glBindTexture(3553, this.scene.getDepthTextureId());
                           GL11.glTexImage2D(3553, 0, format, this.scene.width, this.scene.height, 0, pixel, type, (ByteBuffer)null);
                        }

                        this.scene.setFilterMode(9729);
                     }

                     GL30.glBindFramebuffer(36008, draw);
                     GL30.glBindFramebuffer(36009, this.scene.frameBufferId);
                     if (GL30.glCheckFramebufferStatus(36008) == 36053 && GL30.glCheckFramebufferStatus(36009) == 36053) {
                        for(int i = 0; i < 8 && GL11.glGetError() != 0; ++i) {
                        }

                        GL30.glBlitFramebuffer(viewport[0], viewport[1], viewport[0] + viewport[2], viewport[1] + viewport[3], 0, 0, this.scene.width, this.scene.height, 16384 | (requireDepth ? 256 : 0), 9728);
                        int error = GL11.glGetError();
                        if (error != 0) {
                           RenderDiagnostics.fail("scene copy GL error " + error + ", source " + draw + ", depth " + format);
                           return false;
                        }

                        this.available = true;
                        return true;
                     }

                     RenderDiagnostics.fail("incomplete scene-copy framebuffer");
                     return false;
                  }

                  format = 0;
               } catch (RuntimeException var22) {
                  RenderDiagnostics.fail("scene capture: " + var22.getClass().getSimpleName() + ": " + var22.getMessage());
                  return false;
               } finally {
                  if (sourceReadBuffer != -1) {
                     GL30.glBindFramebuffer(36008, draw);
                     GL11.glReadBuffer(sourceReadBuffer);
                  }

                  GL30.glBindFramebuffer(36008, read);
                  GL30.glBindFramebuffer(36009, draw);
                  RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
                  RenderSystem.clearColor(clear[0], clear[1], clear[2], clear[3]);
                  RenderSystem.activeTexture(active);
                  GL13.glActiveTexture(active);
                  RenderSystem.bindTexture(texture);
                  GL11.glBindTexture(3553, texture);
                  if (scissor) {
                     GL11.glEnable(3089);
                  }

               }

               return false;
            } else {
               RenderDiagnostics.fail("capture exceeds GL_MAX_TEXTURE_SIZE");
               return false;
            }
         }
      } else {
         return false;
      }
   }

   static final class Target implements AutoCloseable {
      private final EffectSamplers samplers = new EffectSamplers();
      private final boolean framebuffers = GL.getCapabilities().OpenGL30 || GL.getCapabilities().GL_ARB_framebuffer_object;
      private final int read = this.framebuffers ? GL11.glGetInteger(36010) : 0;
      private final int draw = this.framebuffers ? GL11.glGetInteger(36006) : 0;
      private final int[] viewport = new int[4];

      Target() {
         GL11.glGetIntegerv(2978, this.viewport);
      }

      public void close() {
         if (this.framebuffers) {
            GL30.glBindFramebuffer(36008, this.read);
            GL30.glBindFramebuffer(36009, this.draw);
         }

         RenderSystem.viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
         this.samplers.close();
      }
   }
}
