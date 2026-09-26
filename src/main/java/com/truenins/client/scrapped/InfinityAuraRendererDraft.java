package com.truenins.client.scrapped;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.truenins.TrueNinsMod;
import com.truenins.register.TNMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class InfinityAuraRendererDraft {

    private static final ResourceLocation TEXTURE = new ResourceLocation("block/glass");
    private static final float RADIUS = 0.95F;
    private static final int SEGMENTS = 36;
    private static final int RINGS = 10;
    private static final int MOTES = 12;
    private static final float MOTE_SIZE = 0.06F;
    private static final int STRIDE = (SEGMENTS + 1) * 4;

    private static final float[] GRID = new float[(RINGS + 1) * (SEGMENTS + 1) * 4];
    private static final List<AbstractClientPlayer> ACTIVE = new ArrayList<>();

    private InfinityAuraRendererDraft() {}

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ACTIVE.clear();
        for (AbstractClientPlayer player : mc.level.players()) {
            if (player.hasEffect(TNMobEffects.INFINITY_FIELD.get())) ACTIVE.add(player);
        }
        if (ACTIVE.isEmpty()) return;

        TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(TEXTURE);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
        VertexConsumer vc = buffers.getBuffer(type);

        Vec3 camera = event.getCamera().getPosition();
        float partial = event.getPartialTick();
        float time = (float) (System.currentTimeMillis() % 600000L) / 1000.0F;

        for (AbstractClientPlayer player : ACTIVE) {
            double ix = Mth.lerp(partial, player.xOld, player.getX());
            double iy = Mth.lerp(partial, player.yOld, player.getY());
            double iz = Mth.lerp(partial, player.zOld, player.getZ());
            double centerY = iy + player.getBbHeight() * 0.5D;

            double dx = camera.x - ix;
            double dy = camera.y - centerY;
            double dz = camera.z - iz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < 1.0E-4D) continue;
            Vec3 toCam = new Vec3(dx / dist, dy / dist, dz / dist);

            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(ix - camera.x, centerY - camera.y, iz - camera.z);
            Matrix4f m = pose.last().pose();

            if (dist > RADIUS * 1.2D) {
                bubble(vc, m, sprite, time, toCam);
            }
            motes(vc, m, sprite, time, toCam);

            pose.popPose();
        }

        buffers.endBatch(type);
    }

    private static void bubble(VertexConsumer vc, Matrix4f m, TextureAtlasSprite sprite,
                               float time, Vec3 toCam) {
        float yaw = time * 0.35F;
        float cy = Mth.cos(yaw);
        float sy = Mth.sin(yaw);
        float r = RADIUS * (1.0F + 0.014F * Mth.sin(time * 1.7F));
        float tx = (float) toCam.x;
        float ty = (float) toCam.y;
        float tz = (float) toCam.z;

        for (int i = 0; i <= RINGS; i++) {
            double phi = Math.PI * i / RINGS;
            float sp = (float) Math.sin(phi);
            float cp = (float) Math.cos(phi);
            for (int j = 0; j <= SEGMENTS; j++) {
                double theta = Math.PI * 2.0D * j / SEGMENTS;
                float ux = sp * Mth.cos((float) theta);
                float uz = sp * Mth.sin((float) theta);
                float rx = ux * cy - uz * sy;
                float rz = ux * sy + uz * cy;
                float facing = Math.abs(rx * tx + cp * ty + rz * tz);
                float alpha = (float) Math.pow(1.0D - facing, 2.4D) * 0.5F;
                int o = i * STRIDE + j * 4;
                GRID[o] = rx * r;
                GRID[o + 1] = cp * r;
                GRID[o + 2] = rz * r;
                GRID[o + 3] = alpha;
            }
        }

        for (int i = 0; i < RINGS; i++) {
            for (int j = 0; j < SEGMENTS; j++) {
                int a0 = i * STRIDE + j * 4;
                int a1 = (i + 1) * STRIDE + j * 4;
                int b1 = (i + 1) * STRIDE + (j + 1) * 4;
                int b0 = i * STRIDE + (j + 1) * 4;
                if (GRID[a0 + 3] + GRID[a1 + 3] + GRID[b1 + 3] + GRID[b0 + 3] < 0.02F) continue;
                float u0 = sprite.getU((float) j / SEGMENTS);
                float u1 = sprite.getU((float) (j + 1) / SEGMENTS);
                float v0 = sprite.getV((float) i / RINGS);
                float v1 = sprite.getV((float) (i + 1) / RINGS);
                gridVertex(vc, m, a0, u0, v0);
                gridVertex(vc, m, a1, u0, v1);
                gridVertex(vc, m, b1, u1, v1);
                gridVertex(vc, m, b0, u1, v0);
            }
        }
    }

    private static void motes(VertexConsumer vc, Matrix4f m, TextureAtlasSprite sprite,
                              float time, Vec3 toCam) {
        Vec3 right = toCam.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
        right = right.normalize();
        Vec3 up = right.cross(toCam).normalize();

        float u0 = sprite.getU(0.0F);
        float u1 = sprite.getU(1.0F);
        float v0 = sprite.getV(0.0F);
        float v1 = sprite.getV(1.0F);

        for (int i = 0; i < MOTES; i++) {
            float ang = i * 6.2831855F / MOTES + time * (0.5F + 0.16F * (i % 3));
            float rr = 0.72F + 0.08F * (i % 3);
            float yy = 0.42F * Mth.sin(ang * 1.6F + i * 1.3F) + 0.05F * (i % 4) - 0.08F;
            float alpha = 0.18F + 0.16F * Mth.sin(time * 2.6F + i * 1.1F);
            if (alpha <= 0.02F) continue;

            float s = MOTE_SIZE * (0.8F + 0.35F * (i % 2));
            Vec3 p = new Vec3(Mth.cos(ang) * rr, yy, Mth.sin(ang) * rr);
            Vec3 ox = right.scale(s);
            Vec3 oy = up.scale(s);

            moteVertex(vc, m, p.subtract(ox).subtract(oy), u0, v0, alpha);
            moteVertex(vc, m, p.add(ox).subtract(oy), u1, v0, alpha);
            moteVertex(vc, m, p.add(ox).add(oy), u1, v1, alpha);
            moteVertex(vc, m, p.subtract(ox).add(oy), u0, v1, alpha);
        }
    }

    private static void gridVertex(VertexConsumer vc, Matrix4f m, int o, float u, float v) {
        vc.vertex(m, GRID[o], GRID[o + 1], GRID[o + 2])
          .color(0.62F, 0.86F, 1.0F, Mth.clamp(GRID[o + 3], 0.0F, 1.0F))
          .uv(u, v)
          .overlayCoords(OverlayTexture.NO_OVERLAY)
          .uv2(0x00F000F0)
          .normal(0.0F, 0.0F, 1.0F)
          .endVertex();
    }

    private static void moteVertex(VertexConsumer vc, Matrix4f m, Vec3 p, float u, float v, float alpha) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z)
          .color(0.78F, 0.94F, 1.0F, Mth.clamp(alpha, 0.0F, 1.0F))
          .uv(u, v)
          .overlayCoords(OverlayTexture.NO_OVERLAY)
          .uv2(0x00F000F0)
          .normal(0.0F, 0.0F, 1.0F)
          .endVertex();
    }
}
