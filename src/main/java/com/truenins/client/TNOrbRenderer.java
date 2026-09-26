package com.truenins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.truenins.TrueNinsMod;
import com.truenins.entity.RedOrbEntity;
import com.truenins.entity.TNOrbEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class TNOrbRenderer extends EntityRenderer<TNOrbEntity> {

    private static final ResourceLocation GLOW =
        new ResourceLocation(TrueNinsMod.MODID, "textures/entity/orb_glow.png");

    private static final float CORE_RADIUS = 0.62F;
    private static final float SHELL_RADIUS = 0.86F;

    public TNOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(TNOrbEntity entity) {
        return GLOW;
    }

    @Override
    public void render(TNOrbEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        boolean red = entity instanceof RedOrbEntity;
        float scale = entity.orbScale();
        if (scale <= 0.01F) return;

        float time = entity.tickCount + partialTick;
        Vec3 camera = this.entityRenderDispatcher.camera.getPosition();
        Vec3 eye = camera.subtract(entity.getX(), entity.getY(), entity.getZ()).normalize();

        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(GLOW));

        int explode = entity.explodeProgress();
        if (explode >= 0) {
            pose.pushPose();
            pose.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            shockwave(pose, vc, eye, explode);
            pose.popPose();
            super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
            return;
        }

        pose.pushPose();
        pose.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        pose.scale(scale, scale, scale);

        if (red) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(time * 26.0F));
            ball(pose, vc, eye, SHELL_RADIUS, 14, 0.16F,
                0.42F, 0.02F, 0.03F, 1.00F, 0.28F, 0.14F, 0.20F, 0.62F);
            ball(pose, vc, eye, CORE_RADIUS, 12, 0.16F,
                0.06F, 0.01F, 0.01F, 1.00F, 0.72F, 0.48F, 0.92F, 0.42F);
            pose.popPose();

            for (int i = 0; i < 2; i++) {
                pose.pushPose();
                pose.mulPose(Axis.XP.rotationDegrees(24.0F + i * 52.0F));
                pose.mulPose(Axis.YP.rotationDegrees(time * (44.0F + i * 26.0F)));
                ring(pose, vc, 1.02F + i * 0.16F, 0.10F, 40,
                    1.00F, 0.22F, 0.10F, 0.42F - i * 0.12F);
                pose.popPose();
            }
        } else {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(time * 82.0F));
            ball(pose, vc, eye, SHELL_RADIUS, 16, 0.05F,
                0.02F, 0.06F, 0.16F, 0.42F, 0.74F, 1.00F, 0.18F, 0.58F);
            ball(pose, vc, eye, CORE_RADIUS, 12, 0.05F,
                0.01F, 0.03F, 0.09F, 1.00F, 1.00F, 1.00F, 0.88F, 0.36F);
            pose.popPose();

            for (int i = 0; i < 3; i++) {
                pose.pushPose();
                pose.mulPose(Axis.XP.rotationDegrees(16.0F + i * 44.0F));
                pose.mulPose(Axis.ZP.rotationDegrees(12.0F * i));
                pose.mulPose(Axis.YP.rotationDegrees(time * (130.0F + i * 44.0F) * (i % 2 == 0 ? 1.0F : -1.0F)));
                ring(pose, vc, 0.98F + i * 0.15F, 0.07F, 48,
                    0.50F, 0.82F, 1.00F, 0.40F - i * 0.10F);
                pose.popPose();
            }
        }

        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }

    private static void shockwave(PoseStack pose, VertexConsumer vc, Vec3 eye, int progress) {
        float p = Mth.clamp(progress / 18.0F, 0.0F, 1.0F);
        float wave = (float) Math.pow(p, 0.42D);
        float fade = 1.0F - p;

        float blast = (float) com.truenins.TrueNinsConfig.heBlastRadius();
        float radius = 0.45F + blast * wave;

        ball(pose, vc, eye, radius, 14, 0.0F,
            0.30F, 0.03F, 0.02F, 1.00F, 0.42F, 0.16F,
            0.04F * fade, 0.52F * fade);

        ball(pose, vc, eye, radius * 0.62F, 10, 0.0F,
            0.05F, 0.01F, 0.01F, 1.00F, 1.00F, 0.86F,
            0.02F * fade, 0.30F * fade);

        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        ring(pose, vc, radius * 1.06F, 0.42F * (1.0F - wave * 0.6F), 44,
            1.00F, 0.34F, 0.12F, 0.55F * fade);
        pose.popPose();

        ball(pose, vc, eye, Math.max(0.05F, CORE_RADIUS * (1.0F - p)), 8, 0.0F,
            0.10F, 0.01F, 0.01F, 1.00F, 0.90F, 0.62F, 0.85F * fade, 0.25F * fade);
    }

    private static void ball(PoseStack pose, VertexConsumer vc, Vec3 eye, float radius, int gradation,
                             float lumpy, float cr, float cg, float cb,
                             float rr, float rg, float rb, float coreAlpha, float rimAlpha) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float pi = (float) Math.PI;
        int rings = gradation;
        int sectors = gradation * 2;

        for (int i = 0; i < rings; i++) {
            float a0 = pi * i / rings;
            float a1 = pi * (i + 1) / rings;
            for (int j = 0; j < sectors; j++) {
                float b0 = (float) (2.0 * Math.PI * j / sectors);
                float b1 = (float) (2.0 * Math.PI * (j + 1) / sectors);

                vertex(vc, m, n, eye, radius, a0, b0, lumpy, cr, cg, cb, rr, rg, rb, coreAlpha, rimAlpha);
                vertex(vc, m, n, eye, radius, a1, b0, lumpy, cr, cg, cb, rr, rg, rb, coreAlpha, rimAlpha);
                vertex(vc, m, n, eye, radius, a1, b1, lumpy, cr, cg, cb, rr, rg, rb, coreAlpha, rimAlpha);
                vertex(vc, m, n, eye, radius, a0, b1, lumpy, cr, cg, cb, rr, rg, rb, coreAlpha, rimAlpha);
            }
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 eye,
                               float radius, float alpha, float beta, float lumpy,
                               float cr, float cg, float cb, float rr, float rg, float rb,
                               float coreAlpha, float rimAlpha) {
        float sa = Mth.sin(alpha);
        float ca = Mth.cos(alpha);
        float sb = Mth.sin(beta);
        float cb2 = Mth.cos(beta);

        float nx = sa * cb2;
        float ny = ca;
        float nz = sa * sb;

        float r = radius;
        if (lumpy > 0.0F) {
            r *= 1.0F + lumpy * Mth.sin(alpha * 5.0F) * Mth.cos(beta * 7.0F)
                + lumpy * 0.6F * Mth.sin(alpha * 11.0F + beta * 4.0F);
        }

        float facing = Math.abs((float) (nx * eye.x + ny * eye.y + nz * eye.z));
        float rim = 1.0F - facing;
        rim = rim * rim * (3.0F - 2.0F * rim);
        rim = (float) Math.pow(rim, 0.85D);

        float vr = cr + (rr - cr) * rim;
        float vg = cg + (rg - cg) * rim;
        float vb = cb + (rb - cb) * rim;
        float va = coreAlpha + (rimAlpha - coreAlpha) * rim;

        vc.vertex(m, nx * r, ny * r, nz * r)
          .color(vr, vg, vb, Mth.clamp(va, 0.0F, 1.0F))
          .uv(0.5F, 0.5F)
          .overlayCoords(OverlayTexture.NO_OVERLAY)
          .uv2(0xF000F0)
          .normal(n, nx, ny, nz)
          .endVertex();
    }

    private static void ring(PoseStack pose, VertexConsumer vc, float radius, float width, int segments,
                             float r, float g, float b, float a) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float inner = Math.max(0.0F, radius - width);

        for (int i = 0; i < segments; i++) {
            double t0 = 2.0D * Math.PI * i / segments;
            double t1 = 2.0D * Math.PI * (i + 1) / segments;

            float ox0 = (float) (Math.cos(t0) * radius);
            float oz0 = (float) (Math.sin(t0) * radius);
            float ox1 = (float) (Math.cos(t1) * radius);
            float oz1 = (float) (Math.sin(t1) * radius);
            float ix0 = (float) (Math.cos(t0) * inner);
            float iz0 = (float) (Math.sin(t0) * inner);
            float ix1 = (float) (Math.cos(t1) * inner);
            float iz1 = (float) (Math.sin(t1) * inner);

            quad(vc, m, n, ox0, oz0, ox1, oz1, ix1, iz1, ix0, iz0, r, g, b, a);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n,
                             float ax, float az, float bx, float bz,
                             float cx, float cz, float dx, float dz,
                             float r, float g, float b, float a) {
        ringVertex(vc, m, n, ax, az, r, g, b, a);
        ringVertex(vc, m, n, bx, bz, r, g, b, a);
        ringVertex(vc, m, n, cx, cz, r, g, b, a * 0.15F);
        ringVertex(vc, m, n, dx, dz, r, g, b, a * 0.15F);
    }

    private static void ringVertex(VertexConsumer vc, Matrix4f m, Matrix3f n,
                                   float x, float z, float r, float g, float b, float a) {
        vc.vertex(m, x, 0.0F, z)
          .color(r, g, b, Mth.clamp(a, 0.0F, 1.0F))
          .uv(0.5F, 0.5F)
          .overlayCoords(OverlayTexture.NO_OVERLAY)
          .uv2(0xF000F0)
          .normal(n, 0.0F, 1.0F, 0.0F)
          .endVertex();
    }
}
