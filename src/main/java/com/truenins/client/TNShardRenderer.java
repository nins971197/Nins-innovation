package com.truenins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.truenins.TrueNinsMod;
import com.truenins.entity.OrbShardEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class TNShardRenderer extends EntityRenderer<OrbShardEntity> {

    private static final ResourceLocation GLOW =
        new ResourceLocation(TrueNinsMod.MODID, "textures/entity/orb_glow.png");

    public TNShardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(OrbShardEntity entity) {
        return GLOW;
    }

    @Override
    public void render(OrbShardEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        BlockState state = entity.blockState();
        if (state.isAir()) return;

        float spin = (entity.tickCount + partialTick) * 1.6F + entity.getId() * 41.0F;

        pose.pushPose();
        pose.translate(0.0D, -0.5D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.translate(-0.5D, 0.0D, -0.5D);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
            state, pose, buffers, 0x00F000F0, OverlayTexture.NO_OVERLAY);
        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }
}
