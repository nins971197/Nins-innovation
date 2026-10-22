package com.truenins.client.domain.shingan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.truenins.TrueNinsMod;
import com.truenins.client.render.PixelGeometry;
import com.truenins.client.render.PixelRenderTypes;
import com.truenins.entity.domain.ShinganDomainEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static com.truenins.client.render.PixelGeometry.*;

/** Local faceted sky and attack tell for the domain. */
public class TNShinganDomainRenderer extends EntityRenderer<ShinganDomainEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(TrueNinsMod.MODID, "textures/effect/pixel.png");
    private static final ResourceLocation SKY = new ResourceLocation(TrueNinsMod.MODID, "textures/effect/domain_sky.png");
    private static final ResourceLocation MIST = new ResourceLocation(TrueNinsMod.MODID, "textures/effect/domain_mist.png");
    public TNShinganDomainRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(ShinganDomainEntity entity) { return TEXTURE; }

    @Override
    public void render(ShinganDomainEntity e, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        if (ShinganSceneEffect.ready()) return;
        float enter = e.appearance(partial);
        if (enter <= .005f) return;
        float time = e.visualAge(partial);
        float open = openingFactor(time);
        float transition = e.transition(partial);
        double r = e.domainRadius() * transition;
        pose.pushPose();
        VertexConsumer sky = buffers.getBuffer(PixelRenderTypes.sky(SKY));
        dome(pose, sky, r, enter, time);
        VertexConsumer mist = buffers.getBuffer(RenderType.entityTranslucentEmissive(MIST));
        haze(pose, mist, r, enter, time);
        VertexConsumer pixels = buffers.getBuffer(RenderType.entityTranslucentEmissive(PIXEL));
        stars(pose, pixels, r, enter, time);
        attackPulse(pose, pixels, e, time, enter);
        if (transition < .999f) {
            double ring = e.domainRadius() * transition;
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI * 2 / 48.0;
                Vec3 from = new Vec3(Math.cos(a) * ring, Math.sin(time * .04 + i) * .05, Math.sin(a) * ring);
                Vec3 to = new Vec3(Math.cos(a + .055) * ring, Math.sin(time * .04 + i + .2) * .05, Math.sin(a + .055) * ring);
                beam(pose, pixels, from, to, .11, i % 3 == 0 ? 0xF08BFF : 0x8E35D7,
                    enter * (1 - transition), FULL_BRIGHT);
            }
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, packedLight);
    }

    private static float openingFactor(float age) {
        float t = Mth.clamp(age / 36.0f, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static void dome(PoseStack p, VertexConsumer out, double radius, float alpha, float time) {
        int around = 64, rows = 36;
        for (int row = 0; row < rows; row++) {
            double phi0 = -Math.PI * .5 + row * (Math.PI / rows), phi1 = -Math.PI * .5 + (row + 1) * (Math.PI / rows);
            for (int i = 0; i < around; i++) {
                double a0 = i * Math.PI * 2 / around, a1 = (i + 1) * Math.PI * 2 / around;
                Vec3 p0 = skyPoint(radius, phi0, a0), p1 = skyPoint(radius, phi0, a1);
                Vec3 p2 = skyPoint(radius, phi1, a1), p3 = skyPoint(radius, phi1, a0);
                float drift = time * .00038f;
                texturedFace(p, out, p0, p1, p2, p3, 0xFFFFFF, alpha, FULL_BRIGHT,
                    i/(float)around + drift, row/(float)rows, (i+1)/(float)around + drift, (row+1)/(float)rows);
            }
        }
    }

    private static void stars(PoseStack p, VertexConsumer out, double radius, float alpha, float time) {
        // Sparse suspended pixels read as distant light, not a particle curtain.
        for (int i = 0; i < 18; i++) {
            double a = i * 2.39996 + time * .002;
            double y = radius * (.22 + ((i * 17) % 61) / 100.0);
            double rr = Math.sqrt(radius * radius - y * y) * .965;
            Vec3 point = new Vec3(Math.cos(a) * rr, y + Math.sin(time * .05 + i) * .15, Math.sin(a) * rr);
            cube(p, out, point, i % 5 == 0 ? .16 : .085, i % 3 == 0 ? 0xFF9BFF : 0xC54BEF,
                alpha * .75f);
        }
    }

    private static Vec3 skyPoint(double radius, double phi, double angle) {
        return new Vec3(radius * Math.cos(phi) * Math.cos(angle), radius * Math.sin(phi),
            radius * Math.cos(phi) * Math.sin(angle));
    }

    private static void haze(PoseStack p, VertexConsumer out, double r, float alpha, float time) {
        for (int i = 0; i < 24; i++) {
            double angle = i * 2.39996 + time * .0015;
            double radius = r * (.28 + (i % 5) * .12);
            double x = Math.cos(angle) * radius, z = Math.sin(angle) * radius;
            double y = .16 + (i % 4) * .32 + Math.sin(time * .03 + i) * .08;
            double size = 5.8 + i % 3;
            face(p,out,new Vec3(x-size,y,z-size),new Vec3(x+size,y,z-size),
                new Vec3(x+size,y,z+size),new Vec3(x-size,y,z+size),0xFFFFFF,alpha*.9f,FULL_BRIGHT);
        }
    }

    private static void attackPulse(PoseStack p, VertexConsumer out, ShinganDomainEntity e,
                                    float time, float alpha) {
        float age = e.pulseAge(0);
        if (age < 0 || age > 15) return;
        double progress = age / 15.0;
        double r = e.domainRadius() * (.2 + progress * .74);
        float opacity = (1 - (float)progress) * alpha * .7f;
        int segments = 56;
        for (int i = 0; i < segments; i += 2) {
            double a = i * Math.PI * 2 / segments, b = (i + 1.6) * Math.PI * 2 / segments;
            Vec3 from = new Vec3(Math.cos(a) * r, .18 + Math.sin(time * .08) * .08, Math.sin(a) * r);
            Vec3 to = new Vec3(Math.cos(b) * r, .18 + Math.sin(time * .08) * .08, Math.sin(b) * r);
            beam(p, out, from, to, .12 * (1 - progress * .4), 0xFF4CB9, opacity, FULL_BRIGHT);
        }
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI * .2 + time * .03;
            cube(p, out, new Vec3(Math.cos(a) * r, .28, Math.sin(a) * r), .13,
                i % 2 == 0 ? 0xFF9ADE : 0xD74DF0, opacity);
        }
    }
}
