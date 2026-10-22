package com.truenins.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** Emissive volumes still need depth: clouds and rear faces must stay behind them. */
public abstract class PixelRenderTypes extends RenderType {
    private static final Function<ResourceLocation, RenderType> SKY = Util.memoize(texture -> create(
        "truenins_pixel_sky", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
        256, false, true, CompositeState.builder()
            .setShaderState(new ShaderStateShard(PixelShaders::sky))
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .createCompositeState(false)));
    private static final Function<ResourceLocation, RenderType> VOLUME = Util.memoize(texture -> create(
        "truenins_pixel_volume", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
        256, false, true, CompositeState.builder()
            .setShaderState(new ShaderStateShard(PixelShaders::volume))
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .createCompositeState(false)));

    private PixelRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode,
                             int size, boolean crumbling, boolean sorted, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sorted, setup, clear);
    }

    public static RenderType volume(ResourceLocation texture) { return VOLUME.apply(texture); }
    public static RenderType sky(ResourceLocation texture) { return SKY.apply(texture); }
}
