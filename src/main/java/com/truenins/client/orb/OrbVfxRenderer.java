package com.truenins.client.orb;

import com.truenins.TrueNinsConfig;
import com.truenins.client.vfx.WorldEffect;
import com.truenins.entity.orb.*;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import static com.truenins.client.vfx.WorldVfxRenderer.vector;

/** Gameplay adapters for the reference Blue, Red and stationary Purple materials. */
public final class OrbVfxRenderer {
    private static ShaderInstance blue, red, purple;
    private OrbVfxRenderer() {}
    public static void setBlue(ShaderInstance value) { blue = value; }
    public static void setRed(ShaderInstance value) { red = value; }
    public static void setPurple(ShaderInstance value) { purple = value; }
    public static boolean ready(TNOrbEntity e) {
        return e instanceof BlueOrbEntity ? blue != null : e instanceof RedOrbEntity ? red != null : purple != null;
    }
    public static WorldEffect effect(Entity entity, float partial) {
        if (!(entity instanceof TNOrbEntity orb) || !ready(orb)) return null;
        float scale = orb.visualScale(partial);
        if (scale < .01f) return null;
        Vec3 center = new Vec3(Mth.lerp(partial, entity.xo, entity.getX()),
            Mth.lerp(partial, entity.yo, entity.getY()), Mth.lerp(partial, entity.zo, entity.getZ()));
        ShaderInstance material = orb instanceof BlueOrbEntity ? blue : orb instanceof RedOrbEntity ? red : purple;
        return new WorldEffect(material, center, 10, !(orb instanceof BlueOrbEntity), false, (shader, frame) -> {
            Vec3 axis = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot()).normalize();
            Vec3 right = axis.cross(Math.abs(axis.y) > .96 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0)).normalize();
            Vec3 up = right.cross(axis).normalize();
            vector(shader, "Axis", axis); vector(shader, "Right", right); vector(shader, "Up", up);
            vector(shader, "FusionRight", right); vector(shader, "FusionUp", up);
            shader.safeGetUniform("Seed").set((float)entity.getId());
            float burst = orb.explodeProgress() < 0 ? 0 : (orb.explodeProgress() + partial) / 20f;
            if (orb instanceof BlueOrbEntity) {
                shader.safeGetUniform("Kind").set(0f);
                shader.safeGetUniform("Phase").set(0f);
                shader.safeGetUniform("PhaseTime").set(0f);
                shader.safeGetUniform("Expansion").set(1f);
                shader.safeGetUniform("CoreRadius").set(1.3f * scale);
                shader.safeGetUniform("Radius").set(3.25f * scale);
            } else if (orb instanceof RedOrbEntity he) {
                shader.safeGetUniform("Phase").set(orb.explodeProgress() >= 0 ? 2f : he.visualFlying() ? 1f : 0f);
                shader.safeGetUniform("PhaseTime").set(burst);
                shader.safeGetUniform("FlightTime").set((entity.tickCount + partial) / 20f);
                shader.safeGetUniform("Charge").set(Mth.clamp(scale, 0, 1));
                shader.safeGetUniform("CoreRadius").set(.45f * scale);
                shader.safeGetUniform("Radius").set((float)TrueNinsConfig.heBlastRadius());
                shader.safeGetUniform("BeamMode").set(0f);
                shader.safeGetUniform("UseDepth").set(1f);
                shader.safeGetUniform("SceneReady").set(1f);
                shader.safeGetUniform("Distortion").set(.65f);
                Vec3 relative = center.subtract(frame.camera());
                Vector4f clip = frame.viewProjection().transform(new Vector4f((float)relative.x, (float)relative.y, (float)relative.z, 1));
                float w = Math.max(.001f, Math.abs(clip.w));
                float projectionY = frame.projectionY();
                shader.safeGetUniform("CoreScreen").set(clip.x / w * .5f + .5f, clip.y / w * .5f + .5f,
                    projectionY * .5f / w, 1f);
            } else if (orb instanceof PurpleOrbEntity shiki) {
                float age = shiki.explodeProgress() >= 0 ? 1f + burst
                    : (shiki.phase() + partial) / Math.max(1f, TrueNinsConfig.purpleFusionTicks());
                shader.safeGetUniform("Age").set(age);
                shader.safeGetUniform("FeedbackScale").set((float)(double)net.minecraft.client.Minecraft.getInstance().options.screenEffectScale().get());
                shader.safeGetUniform("Radius").set((float)TrueNinsConfig.purpleBlastRadius());
            }
        });
    }
}
