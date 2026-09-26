package com.truenins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.truenins.TrueNinsMod;
import com.truenins.register.TNMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT)
public final class MadeInHeavenRenderer {

    private static final ResourceLocation SUN =
        new ResourceLocation(TrueNinsMod.MODID, "textures/particle/time_sun.png");
    private static final ResourceLocation MOON =
        new ResourceLocation(TrueNinsMod.MODID, "textures/particle/time_moon.png");
    private static final ResourceLocation RING =
        new ResourceLocation(TrueNinsMod.MODID, "textures/particle/time_ring.png");

    private static final float ORBIT_RADIUS = 1.45F;
    private static final float RING_RADIUS = 1.80F;
    private static final double ORBIT_HEIGHT = 1.05D;
    private static final double RANGE = 48.0D;

    private static final List<AbstractClientPlayer> ACTIVE = new ArrayList<>();

    private MadeInHeavenRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ACTIVE.clear();
        for (AbstractClientPlayer player : mc.level.players()) {
            if (player.hasEffect(TNMobEffects.MADE_IN_HEAVEN.get())) ACTIVE.add(player);
        }
        if (ACTIVE.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);

        RenderType ringType = RenderType.entityTranslucent(RING);
        RenderType sunType = RenderType.entityTranslucent(SUN);
        RenderType moonType = RenderType.entityTranslucent(MOON);
        VertexConsumer ringVc = buffers.getBuffer(ringType);
        VertexConsumer sunVc = buffers.getBuffer(sunType);
        VertexConsumer moonVc = buffers.getBuffer(moonType);

        PoseStack pose = event.getPoseStack();
        double theta = (mc.level.getDayTime() % 24000L) / 24000.0D * Math.PI * 2.0D;

        for (AbstractClientPlayer player : ACTIVE) {
            if (player.distanceToSqr(mc.player) > RANGE * RANGE) continue;

            double ix = Mth.lerp(partial, player.xOld, player.getX());
            double iy = Mth.lerp(partial, player.yOld, player.getY());
            double iz = Mth.lerp(partial, player.zOld, player.getZ());

            pose.pushPose();
            pose.translate(ix - camera.x, iy - camera.y, iz - camera.z);

            pose.pushPose();
            pose.translate(0.0D, 0.16D, 0.0D);
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(theta * 1.7D)));
            Matrix4f rm = pose.last().pose();
            flatQuad(ringVc, rm, RING_RADIUS, 0.60F);
            flatQuadReversed(ringVc, rm, RING_RADIUS, 0.60F);
            pose.popPose();

            Vec3 sunPos = new Vec3(ix + Math.cos(theta) * ORBIT_RADIUS, iy + ORBIT_HEIGHT,
                iz + Math.sin(theta) * ORBIT_RADIUS);
            Vec3 moonPos = new Vec3(ix + Math.cos(theta + Math.PI) * ORBIT_RADIUS, iy + ORBIT_HEIGHT,
                iz + Math.sin(theta + Math.PI) * ORBIT_RADIUS);

            billboard(sunVc, pose, camera, worldUp, sunPos, 0.66F, 1.0F);
            billboard(moonVc, pose, camera, worldUp, moonPos, 0.54F, 1.0F);

            pose.popPose();
        }

        buffers.endBatch(ringType);
        buffers.endBatch(sunType);
        buffers.endBatch(moonType);
    }

    private static void billboard(VertexConsumer vc, PoseStack pose, Vec3 camera, Vec3 worldUp,
                                  Vec3 pos, float size, float alpha) {
        Vec3 toCam = camera.subtract(pos);
        double len = toCam.length();
        if (len < 1.0E-4D) return;
        toCam = toCam.scale(1.0D / len);

        Vec3 right = toCam.cross(worldUp);
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
        right = right.normalize();
        Vec3 up = right.cross(toCam).normalize();

        pose.pushPose();
        pose.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
        Matrix4f m = pose.last().pose();
        float half = size * 0.5F;
        Vec3 rx = right.scale(half);
        Vec3 uy = up.scale(half);

        Vec3 nx = rx.scale(-1.0D);
        quad(vc, m, nx.subtract(uy), rx.subtract(uy), rx.add(uy), nx.add(uy), alpha);
        quad(vc, m, nx.add(uy), rx.add(uy), rx.subtract(uy), nx.subtract(uy), alpha);
        pose.popPose();
    }

    private static void flatQuad(VertexConsumer vc, Matrix4f m, float r, float alpha) {
        quad(vc, m,
            new Vec3(-r, -r, 0.0D), new Vec3(r, -r, 0.0D),
            new Vec3(r, r, 0.0D), new Vec3(-r, r, 0.0D), alpha);
    }

    private static void flatQuadReversed(VertexConsumer vc, Matrix4f m, float r, float alpha) {
        quad(vc, m,
            new Vec3(-r, r, 0.0D), new Vec3(r, r, 0.0D),
            new Vec3(r, -r, 0.0D), new Vec3(-r, -r, 0.0D), alpha);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float alpha) {
        vertex(vc, m, p0, 0.0F, 0.0F, alpha);
        vertex(vc, m, p1, 1.0F, 0.0F, alpha);
        vertex(vc, m, p2, 1.0F, 1.0F, alpha);
        vertex(vc, m, p3, 0.0F, 1.0F, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p, float u, float v, float alpha) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z)
          .color(1.0F, 1.0F, 1.0F, Mth.clamp(alpha, 0.0F, 1.0F))
          .uv(u, v)
          .overlayCoords(OverlayTexture.NO_OVERLAY)
          .uv2(0x00F000F0)
          .normal(0.0F, 0.0F, 1.0F)
          .endVertex();
    }
}
