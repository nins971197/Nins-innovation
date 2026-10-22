package com.truenins.client.domain.judgment;

import com.truenins.client.vfx.WorldEffect;
import com.truenins.entity.domain.JudgmentDomainEntity;
import com.truenins.spell.domain.JudgmentRules;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import static com.truenins.client.vfx.WorldVfxRenderer.vector;
import static com.truenins.entity.domain.JudgmentDomainEntity.smooth;

/** The same scene/depth pipeline as Blue and Red, with a tapered golden sword material. */
public final class ExecutionerSwordEffect {
    private static ShaderInstance shader;
    private ExecutionerSwordEffect() {}
    public static void setShader(ShaderInstance value) { shader = value; }
    public static WorldEffect effect(Entity entity, float partial) {
        if (!(entity instanceof JudgmentDomainEntity d) || shader == null || d.verdict() != JudgmentRules.DEATH) return null;
        float age = d.visualAge(partial);
        if (age < 173 || age >= 232) return null;
        float formed = smooth((age - 173) / 20), swing = smooth((age - 201) / 11);
        float opacity = formed * (1 - smooth((age - 216) / 16)) * d.visibility(partial);
        if (opacity < .001f) return null;
        double angle = Math.toRadians(-65 + swing * 132);
        Vec3 axis = d.local(-Math.sin(angle), Math.cos(angle), 0);
        Vec3 right = d.local(Math.cos(angle), Math.sin(angle), 0);
        return new WorldEffect(shader, d.position().add(d.local(0, 2.8, 3.7)), 10, false, false, (s, frame) -> {
            vector(s, "Axis", axis); vector(s, "Right", right);
            vector(s, "Normal", d.local(0, 0, 1));
            s.safeGetUniform("Formation").set(formed);
            s.safeGetUniform("Visibility").set(opacity);
            s.safeGetUniform("Age").set(age);
        });
    }
}
