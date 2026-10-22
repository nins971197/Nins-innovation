package com.truenins.client.orb;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.truenins.TrueNinsMod;
import com.truenins.entity.orb.OrbShardEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

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
        BlockPos pos = entity.blockPosition();
        RandomSource random = RandomSource.create(state.getSeed(pos));
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);

        pose.pushPose();
        pose.translate(0.0D, -0.5D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.translate(-0.5D, 0.0D, -0.5D);
        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            minecraft.getBlockRenderer().renderSingleBlock(
                state, pose, buffers, 0x00F000F0, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, renderType);
        }
        pose.popPose();

        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }
}
