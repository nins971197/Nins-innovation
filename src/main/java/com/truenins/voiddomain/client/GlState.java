package com.truenins.voiddomain.client;

import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.systems.RenderSystem;
import com.truenins.mixin.EffectBlendModeAccessor;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final class GlState {
   private GlState() {
   }

   public static final class State implements AutoCloseable {
      private final BlendMode cachedBlend = EffectBlendModeAccessor.truenins$getLastApplied();
      private final EffectSamplers samplers = new EffectSamplers();
      final ShaderInstance shader = RenderSystem.getShader();
      final boolean blend = GL11.glIsEnabled(3042);
      final boolean cull = GL11.glIsEnabled(2884);
      final boolean depth = GL11.glIsEnabled(2929);
      final boolean write = GL11.glGetBoolean(2930);
      final int func = GL11.glGetInteger(2932);
      final int sr = GL11.glGetInteger(32969);
      final int dr = GL11.glGetInteger(32968);
      final int sa = GL11.glGetInteger(32971);
      final int da = GL11.glGetInteger(32970);
      final boolean stencil = GL11.glIsEnabled(2960);
      final int equation = GL11.glGetInteger(32777);
      final int alphaEquation = GL11.glGetInteger(34877);
      final int[] colorMask = new int[4];

      public State() {
         if (this.blend) {
            RenderSystem.enableBlend();
         } else {
            RenderSystem.disableBlend();
         }

         if (this.cull) {
            RenderSystem.enableCull();
         } else {
            RenderSystem.disableCull();
         }

         if (this.depth) {
            RenderSystem.enableDepthTest();
         } else {
            RenderSystem.disableDepthTest();
         }

         RenderSystem.depthMask(this.write);
         RenderSystem.depthFunc(this.func);
         RenderSystem.blendFuncSeparate(this.sr, this.dr, this.sa, this.da);
         GL11.glGetIntegerv(3107, this.colorMask);
         RenderSystem.colorMask(true, true, true, true);
         GL11.glColorMask(true, true, true, true);
         GL11.glDisable(2960);
         RenderSystem.blendEquation(32774);
      }

      @Override
      public void close() {
         RenderSystem.depthMask(this.write);
         RenderSystem.depthFunc(this.func);
         if (this.depth) {
            RenderSystem.enableDepthTest();
         } else {
            RenderSystem.disableDepthTest();
         }

         if (this.cull) {
            RenderSystem.enableCull();
         } else {
            RenderSystem.disableCull();
         }

         RenderSystem.blendFuncSeparate(this.sr, this.dr, this.sa, this.da);
         if (this.blend) {
            RenderSystem.enableBlend();
         } else {
            RenderSystem.disableBlend();
         }

         RenderSystem.setShader(() -> this.shader);
         RenderSystem.colorMask(this.colorMask[0] != 0, this.colorMask[1] != 0, this.colorMask[2] != 0, this.colorMask[3] != 0);
         GL11.glColorMask(this.colorMask[0] != 0, this.colorMask[1] != 0, this.colorMask[2] != 0, this.colorMask[3] != 0);
         if (this.stencil) {
            GL11.glEnable(2960);
         } else {
            GL11.glDisable(2960);
         }

         RenderSystem.blendEquation(this.equation);
         GL20.glBlendEquationSeparate(this.equation, this.alphaEquation);
         this.samplers.close();
         EffectBlendModeAccessor.truenins$setLastApplied(this.cachedBlend);
      }
   }
}
