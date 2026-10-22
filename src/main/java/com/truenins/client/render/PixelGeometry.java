package com.truenins.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.truenins.TrueNinsMod;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Small, shared geometry vocabulary. All coordinates are local world units. */
public final class PixelGeometry {
    public static final ResourceLocation PIXEL = new ResourceLocation(TrueNinsMod.MODID, "textures/effect/pixel.png");
    public static final ResourceLocation STONE = new ResourceLocation(TrueNinsMod.MODID, "textures/effect/domain_stone.png");
    public static final int FULL_BRIGHT = 0xF000F0;

    private PixelGeometry() {}

    public static void vertex(PoseStack pose, VertexConsumer out, Vec3 p, float u, float v,
                              int color, float alpha, int light, float nx, float ny, float nz) {
        out.vertex(pose.last().pose(), (float) p.x, (float) p.y, (float) p.z)
            .color((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, alpha)
            .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
            .normal(pose.last().normal(), nx, ny, nz).endVertex();
    }

    public static void face(PoseStack pose, VertexConsumer out, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                            int color, float alpha, int light) {
        texturedFace(pose,out,a,b,c,d,color,alpha,light,0,0,1,1);
    }

    public static void texturedFace(PoseStack pose, VertexConsumer out, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                            int color, float alpha, int light, float u0, float v0, float u1, float v1) {
        Vec3 normal = b.subtract(a).cross(c.subtract(a)).normalize();
        float nx = (float) normal.x, ny = (float) normal.y, nz = (float) normal.z;
        vertex(pose, out, a, u0, v0, color, alpha, light, nx, ny, nz);
        vertex(pose, out, b, u1, v0, color, alpha, light, nx, ny, nz);
        vertex(pose, out, c, u1, v1, color, alpha, light, nx, ny, nz);
        vertex(pose, out, d, u0, v1, color, alpha, light, nx, ny, nz);
    }

    private static void tiledFace(PoseStack pose, VertexConsumer out, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                                  int color, float alpha, int light) {
        texturedFace(pose, out, a, b, c, d, color, alpha, light, 0, 0,
            (float)a.distanceTo(b), (float)b.distanceTo(c));
    }

    public static void box(PoseStack p, VertexConsumer out, double x, double y, double z,
                           double sx, double sy, double sz, int color, float alpha, int light) {
        Vec3 a = new Vec3(x, y, z), b = new Vec3(x + sx, y, z), c = new Vec3(x + sx, y + sy, z), d = new Vec3(x, y + sy, z);
        Vec3 e = new Vec3(x, y, z + sz), f = new Vec3(x + sx, y, z + sz), g = new Vec3(x + sx, y + sy, z + sz), h = new Vec3(x, y + sy, z + sz);
        tiledFace(p, out, b, a, d, c, shade(color, .83f), alpha, light);
        tiledFace(p, out, e, f, g, h, color, alpha, light);
        tiledFace(p, out, a, e, h, d, shade(color, .72f), alpha, light);
        tiledFace(p, out, f, b, c, g, shade(color, .9f), alpha, light);
        tiledFace(p, out, d, h, g, c, color, alpha, light);
        tiledFace(p, out, a, b, f, e, shade(color, .62f), alpha, light);
    }

    public static void cube(PoseStack p, VertexConsumer out, Vec3 center, double size, int color, float alpha) {
        box(p, out, center.x - size/2, center.y - size/2, center.z - size/2,
            size, size, size, color, alpha, FULL_BRIGHT);
    }

    /** Square cross section, including end caps, so strokes retain depth from every angle. */
    public static void beam(PoseStack p, VertexConsumer out, Vec3 from, Vec3 to, double width,
                            int color, float alpha, int light) {
        Vec3 axis = to.subtract(from).normalize();
        if (axis.lengthSqr() < 1e-8) return;
        Vec3 u = axis.cross(Math.abs(axis.y) > .95 ? new Vec3(1,0,0) : new Vec3(0,1,0)).normalize().scale(width/2);
        Vec3 v = axis.cross(u).normalize().scale(width/2);
        Vec3 a = from.add(u).add(v), b = from.subtract(u).add(v), c = from.subtract(u).subtract(v), d = from.add(u).subtract(v);
        Vec3 e = to.add(u).add(v), f = to.subtract(u).add(v), g = to.subtract(u).subtract(v), h = to.add(u).subtract(v);
        face(p,out,a,b,f,e,color,alpha,light); face(p,out,b,c,g,f,shade(color,.8f),alpha,light);
        face(p,out,c,d,h,g,shade(color,.65f),alpha,light); face(p,out,d,a,e,h,shade(color,.9f),alpha,light);
        face(p,out,d,c,b,a,color,alpha,light); face(p,out,e,f,g,h,color,alpha,light);
    }

    public static int shade(int color, float factor) {
        return ((int)((color >> 16 & 255)*factor) << 16) | ((int)((color >> 8 & 255)*factor) << 8) | (int)((color & 255)*factor);
    }
}
