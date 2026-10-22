package com.truenins.client.vfx;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;

/** Material pass with a world anchor; gameplay never depends on this descriptor. */
public record WorldEffect(ShaderInstance shader, Vec3 center, int layer, boolean composite,
                          boolean occludesWorld, Uniforms uniforms) {
    @FunctionalInterface
    public interface Uniforms {
        void bind(ShaderInstance shader, WorldVfxRenderer.Frame frame);
    }
}
