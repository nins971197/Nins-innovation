package com.truenins.voiddomain.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

final class EffectTarget extends TextureTarget {
   EffectTarget(int width, int height, boolean depth, boolean mac) {
      super(width, height, depth, mac);
      int active = GL11.glGetInteger(34016);
      int texture = GL11.glGetInteger(32873);
      singleLevel(this.getColorTextureId());
      if (depth) {
         singleLevel(this.getDepthTextureId());
      }

      RenderSystem.activeTexture(active);
      GL13.glActiveTexture(active);
      RenderSystem.bindTexture(texture);
      GL11.glBindTexture(3553, texture);
   }

   private static void singleLevel(int texture) {
      RenderSystem.bindTexture(texture);
      GL11.glBindTexture(3553, texture);
      GL11.glTexParameteri(3553, 33084, 0);
      GL11.glTexParameteri(3553, 33085, 0);
   }

   public void destroyBuffers() {
      int read = GL11.glGetInteger(36010);
      int draw = GL11.glGetInteger(36006);
      int owned = this.frameBufferId;
      super.destroyBuffers();
      GL30.glBindFramebuffer(36008, read == owned ? 0 : read);
      GL30.glBindFramebuffer(36009, draw == owned ? 0 : draw);
   }
}
