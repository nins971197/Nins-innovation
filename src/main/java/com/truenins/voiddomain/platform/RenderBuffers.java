package com.truenins.voiddomain.platform;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

public final class RenderBuffers {
   private RenderBuffers() {
   }

   public static BufferBuilder begin(VertexFormat.Mode mode, VertexFormat format) {
      return begin(Tesselator.getInstance().getBuilder(), mode, format);
   }

   public static BufferBuilder begin(BufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format) {
      buffer.begin(mode, format);
      return buffer;
   }
}
