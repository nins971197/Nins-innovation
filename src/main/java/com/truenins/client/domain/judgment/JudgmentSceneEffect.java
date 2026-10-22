package com.truenins.client.domain.judgment;

import com.truenins.client.vfx.WorldEffect;
import com.truenins.entity.domain.JudgmentDomainEntity;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.Entity;

/** Bounded scene capture shared with the other domains; no global time/weather mutation. */
public final class JudgmentSceneEffect {
    private static ShaderInstance shader;
    private JudgmentSceneEffect() {}
    public static void setShader(ShaderInstance value) { shader = value; }
    public static WorldEffect effect(Entity entity, float partial) {
        if (!(entity instanceof JudgmentDomainEntity d) || shader == null) return null;
        float age = d.visualAge(partial), visibility = d.visibility(partial);
        if (visibility < .001f) return null;
        return new WorldEffect(shader, d.position(), 0, true, true, (s, frame) -> {
            JudgmentStageCapture.capture(d, frame);
            s.setSampler("StageColor", JudgmentStageCapture.color());
            s.setSampler("StageDepth", JudgmentStageCapture.depth());
            s.safeGetUniform("Cold").set(JudgmentTimeline.cold(age)?1f:0f);
            com.truenins.client.vfx.WorldVfxRenderer.vector(s,"CourtForward",d.local(0,0,1));
            s.safeGetUniform("Age").set(age);
            s.safeGetUniform("Radius").set(JudgmentDomainEntity.FIELD_RADIUS);
            s.safeGetUniform("Visibility").set(visibility);
        });
    }
}
