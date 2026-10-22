package com.truenins.voiddomain.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;

public class EffectShader extends ShaderInstance {
   private final Map<String, EffectShader.Binding> bindings = new LinkedHashMap();

   public EffectShader(ResourceProvider provider, ResourceLocation name, VertexFormat format) throws IOException {
      super(provider, name, format);
   }

   public void setSampler(String name, Object image) {
      super.setSampler(name, image);
      EffectShader.Binding binding = (EffectShader.Binding)this.bindings.get(name);
      if (binding == null) {
         this.bindings.put(name, new EffectShader.Binding(GL20.glGetUniformLocation(this.getId(), name), image));
      } else {
         binding.image = image;
      }

   }

   @Override
   public void apply() {
      GL20.glUseProgram(this.getId());
      super.apply();
      GL20.glUseProgram(this.getId());
      int active = GL11.glGetInteger(34016);
      int unit = 0;
      boolean samplers = GL.getCapabilities().glBindSampler != 0L;

      for(EffectShader.Binding binding : this.bindings.values()) {
         if (binding.location >= 0) {
            Object image = binding.image;
            int var10000;
            if (image instanceof RenderTarget) {
               RenderTarget target = (RenderTarget)image;
               var10000 = target.getColorTextureId();
            } else if (image instanceof AbstractTexture) {
               AbstractTexture target = (AbstractTexture)image;
               var10000 = target.getId();
            } else if (image instanceof Integer) {
               Integer id = (Integer)image;
               var10000 = id;
            } else {
               var10000 = 0;
            }

            int texture = var10000;
            GL20.glUniform1i(binding.location, unit);
            RenderSystem.activeTexture('\u84c0' + unit);
            GL13.glActiveTexture('\u84c0' + unit);
            RenderSystem.bindTexture(texture);
            GL11.glBindTexture(3553, texture);
            if (samplers) {
               GL33.glBindSampler(unit, 0);
            }

            ++unit;
         }
      }

      RenderSystem.activeTexture(active);
      GL13.glActiveTexture(active);
   }

   private static final class Binding {
      final int location;
      Object image;

      Binding(int location, Object image) {
         this.location = location;
         this.image = image;
      }
   }
}
