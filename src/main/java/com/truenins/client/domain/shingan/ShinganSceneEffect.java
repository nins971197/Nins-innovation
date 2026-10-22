package com.truenins.client.domain.shingan;

import com.truenins.client.vfx.WorldEffect;
import com.truenins.entity.domain.ShinganDomainEntity;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.Entity;

/** Reference-style bounded scene pass; the swords, crosses and ropes keep their real geometry. */
public final class ShinganSceneEffect {
    private static ShaderInstance shader;
    private ShinganSceneEffect() {}
    public static void setShader(ShaderInstance value) { shader = value; }
    public static boolean ready() { return shader != null; }
    public static WorldEffect effect(Entity entity, float partial) {
        if (!(entity instanceof ShinganDomainEntity domain) || shader == null) return null;
        float appearance = domain.appearance(partial), expansion = domain.transition(partial);
        if (appearance < .001f || expansion < .001f) return null;
        return new WorldEffect(shader, domain.position(), 0, true, true, (material, frame) -> {
            material.safeGetUniform("Radius").set(Math.max(.025f, domain.domainRadius() * expansion));
            material.safeGetUniform("Visibility").set(appearance);
            material.safeGetUniform("Age").set(domain.visualAge(partial) / 20f);
            material.safeGetUniform("PulseAge").set(domain.pulseAge(partial) / 20f);
            material.safeGetUniform("Expansion").set(expansion);
        });
    }
}
