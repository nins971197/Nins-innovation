package com.truenins.client.orb;

import com.mojang.blaze3d.vertex.PoseStack;
import com.truenins.client.render.PixelGeometry;
import com.truenins.entity.orb.RedOrbEntity;
import com.truenins.entity.orb.PurpleOrbEntity;
import com.truenins.entity.orb.TNOrbEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Entity adapter; technique choreography is independent of entity registration. */
public final class TNOrbRenderer extends EntityRenderer<TNOrbEntity> {
    public TNOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override public ResourceLocation getTextureLocation(TNOrbEntity entity) { return PixelGeometry.PIXEL; }

    @Override
    public void render(TNOrbEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        if (OrbVfxRenderer.ready(entity)) return;
        float scale = entity.visualScale(partialTick);
        if (scale < .01f) return;
        // World time continues for late trackers and does not depend on server-only life counters.
        float time = (entity.level().getGameTime() % 24000) + partialTick;
        pose.pushPose();
        pose.scale(scale, scale, scale);
        if (entity instanceof PurpleOrbEntity purple) {
            TechniqueEffects.purple(pose, buffers, time, purple.phase(), purple.explodeProgress());
        } else if (entity instanceof RedOrbEntity red) {
            TechniqueEffects.red(pose, buffers, time, red.visualDirection(), red.visualFlying(),
                red.explodeProgress() < 0 ? -1 : red.explodeProgress() + partialTick);
        } else {
            TechniqueEffects.blue(pose, buffers, time);
        }
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }
}
