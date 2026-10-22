package com.truenins.client.domain.shingan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.truenins.client.render.PixelGeometry;
import com.truenins.client.render.PixelRenderTypes;
import com.truenins.entity.domain.ShinganDecorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static com.truenins.client.render.PixelGeometry.*;

/** Thick, deterministic, pixel-built props for the domain scene. */
public class TNShinganDecorRenderer extends EntityRenderer<ShinganDecorEntity> {
    public TNShinganDecorRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public ResourceLocation getTextureLocation(ShinganDecorEntity entity) { return STONE; }

    @Override
    public void render(ShinganDecorEntity e, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        float enter = e.appearance(partial);
        if (enter < .015f) return;
        float alpha = Math.min(1, enter * 1.4f);
        float emerge = e.emergence(partial);
        float age = ((e.level().getGameTime() % 24000) + partial) * .05f;
        int variant = e.variant();
        VertexConsumer solid = buffers.getBuffer(PixelRenderTypes.volume(STONE));
        // Use one batch: requesting another render type can flush the previous consumer.
        VertexConsumer color = solid;
        long hash = e.getUUID().getMostSignificantBits();
        float scale = variant == 1 ? .66f + (int)(hash & 3) * .11f : variant == 0 ? .88f + (int)(hash & 3) * .12f : 1;
        if (variant == 2 && e.ropeEnd().lengthSqr() > .01) {
            connectingRopes(pose, color, e.ropeEnd(), alpha, age);
        }
        pose.pushPose();
        pose.translate(0, (variant == 2 ? -1.5 : -3.8) * (1 - emerge), 0);
        pose.scale(.72f + .28f * emerge, .18f + .82f * emerge, .72f + .28f * emerge);
        pose.mulPose(Axis.YP.rotationDegrees(180 - e.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot() + (1-emerge) * 16));
        pose.scale(scale, scale, scale);
        if (variant == 0) katana(pose, solid, color, alpha);
        else if (variant == 1) cross(pose, solid, alpha, hash);
        else knot(pose, color, alpha, age, hash);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, packedLight);
    }

    private static void katana(PoseStack p, VertexConsumer stone, VertexConsumer color, float a) {
        // Blade rises from the ground; a dark spine, pale cutting edge and a readable wrapped grip.
        box(p, stone, -.105, -.12, -.035, .21, 2.88, .07, 0xD9D7E3, a, FULL_BRIGHT);
        box(p, color, -.025, -.1, -.046, .05, 2.8, .015, 0xFFF5F7, a, FULL_BRIGHT);
        box(p, color, -.34, 2.69, -.22, .68, .105, .44, 0x9B7540, a, FULL_BRIGHT);
        box(p, color, -.08, 2.78, -.08, .16, .82, .16, 0x14131A, a, FULL_BRIGHT);
        // Stepped diagonal wrap reads as individual Minecraft pixels from a distance.
        for (int i = 0; i < 8; i++) {
            double y = 2.82 + i * .092;
            double x = (i % 2 == 0) ? -.075 : .018;
            box(p, color, x, y, -.087, .057, .048, .174, 0xE9C3D6, a, FULL_BRIGHT);
        }
        box(p, color, -.105, 3.58, -.105, .21, .13, .21, 0x9B7540, a, FULL_BRIGHT);
    }

    private static void cross(PoseStack p, VertexConsumer out, float a, long seed) {
        double lean = ((seed >>> 4) & 1) == 0 ? -.18 : .16;
        p.mulPose(Axis.ZP.rotation((float)lean));
        double h = 4.1 + ((seed >>> 7) & 3) * .38;
        int form = (int)((seed >>> 18) & 3);
        double armY = h * (.68 + ((seed >>> 10) & 3) * .035);
        double top = form == 3 ? Math.max(h * .70, armY + .48) : h;
        box(p, out, -.25, .05, -.22, .5, top, .44, 0xECEAF2, a, FULL_BRIGHT);
        if (form == 2 || form == 3) {
            // Broken silhouette: omit an arm instead of painting a crack over a complete cross.
            box(p, out, -.24, armY, -.25, 1.66, .48, .5, 0xF4F1F8, a, FULL_BRIGHT);
            box(p, out, -.59, armY+.08, -.21, .35, .27, .42, 0xCDC8D4, a, FULL_BRIGHT);
            box(p, out, -.95, .03, .40, .56, .31, .48, 0xD7D2DE, a, FULL_BRIGHT);
        } else {
            box(p, out, -1.42, armY, -.25, 2.84, .48, .5, 0xF4F1F8, a, FULL_BRIGHT);
        }
        if (form == 1 || form == 3) {
            // Six directions: vertical spine, left/right and front/back stone arms.
            box(p, out, -.24, armY-.08, -1.32, .48, .5, form == 3 ? 1.56 : 2.64,
                0xE7E3ED, a, FULL_BRIGHT);
        }
        // Offset shoulder blocks give the silhouette chipped, stacked stone depth.
        box(p, out, -.43, armY - .13, -.32, .86, .66, .64, 0xE6E3EC, a, FULL_BRIGHT);
        box(p, out, -.2, h * .31, -.31, .4, .68, .62, 0xF7F4FA, a, FULL_BRIGHT);
        double breakAt = h * (.39 + ((seed >>> 13) & 3) * .045);
        box(p, out, -.22, breakAt, -.4, .44, .13, .8, 0xC9C5D1, a, FULL_BRIGHT);
        // Staggered fragments suggest fractured monuments while preserving the cross read.
        box(p, out, -.38, h * .49, .04, .22, .34, .34, 0xD9D6E1, a, FULL_BRIGHT);
        box(p, out, .21, h * .50, -.31, .23, .3, .28, 0xD2CFDA, a, FULL_BRIGHT);
    }

    private static void knot(PoseStack p, VertexConsumer out, float a, float time, long seed) {
        float sway = (float)Math.sin(time + (seed & 31)) * .11f;
        p.translate(0, .08 * Math.sin(time * .7 + (seed & 15)), 0);
        p.mulPose(Axis.YP.rotation(sway));
        // Two interwoven, open loops, assembled from square rope segments with crossing depth.
        loop(p, out, 0xD82145, a, .88, .39, -.65, 0);
        loop(p, out, 0xF3E9F1, a, .88, .39, .65, Math.PI);
        Vec3 redA = new Vec3(-.18, .06, .02), redB = new Vec3(-.36, -1.42, .16);
        Vec3 whiteA = new Vec3(.18, .02, -.03), whiteB = new Vec3(.48, -1.68, -.12);
        ribbon(p, out, redA, redB, .105, 0xD82145, a, time, 0);
        ribbon(p, out, whiteA, whiteB, .09, 0xF3E9F1, a, time, 1.4);
        ribbon(p, out, new Vec3(-.56,.43,0), new Vec3(-.88,1.98,.05), .095, 0xD82145, a, time, .7);
        ribbon(p, out, new Vec3(.57,.42,0), new Vec3(.78,1.87,-.05), .085, 0xF3E9F1, a, time, 2.1);
    }

    private static void loop(PoseStack p, VertexConsumer out, int color, float alpha,
                             double rx, double ry, double tilt, double phase) {
        Vec3 previous = null;
        for (int i = 0; i <= 24; i++) {
            double t = phase + i * Math.PI * 2 / 24;
            double x = Math.cos(t) * rx, y = Math.sin(t) * ry;
            Vec3 next = new Vec3(x * Math.cos(tilt) - y * Math.sin(tilt),
                x * Math.sin(tilt) + y * Math.cos(tilt), .16 * Math.sin(t * 2 + phase));
            if (previous != null) beam(p, out, previous, next, .105, color, alpha, FULL_BRIGHT);
            previous = next;
        }
    }

    private static void ribbon(PoseStack p, VertexConsumer out, Vec3 from, Vec3 to, double width,
                               int color, float alpha, float time, double phase) {
        Vec3 last = from;
        for (int i = 1; i <= 12; i++) {
            double t = i / 12.0;
            Vec3 next = from.lerp(to, t).add(Math.sin(time + t * 5 + phase) * .08, 0, Math.cos(time * .8 + t * 4 + phase) * .08);
            beam(p, out, last, next, width * (1 - .45 * t), color, alpha, FULL_BRIGHT);
            last = next;
        }
    }

    private static void connectingRopes(PoseStack p, VertexConsumer out, Vec3 end, float a, float time) {
        int segments = Math.min(40, Math.max(12, (int)(end.length() * 2)));
        double sag = Math.min(2.2, end.length() * .16);
        for (int strand = 0; strand < 2; strand++) {
            Vec3 last = new Vec3(0, .14 + strand * .16, 0);
            for (int i = 1; i <= segments; i++) {
                double t = i / (double)segments;
                double wave = Math.sin(Math.PI * t);
                Vec3 next = end.scale(t).add(Math.sin(time + t * 5) * .08 * wave,
                    .14 + strand * .16 - sag * 4 * t * (1 - t),
                    Math.cos(time * .8 + t * 4) * .09 * wave);
                beam(p, out, last, next, .075, strand == 0 ? 0xB51C3E : 0xF0DFEB, a, FULL_BRIGHT);
                last = next;
            }
        }
    }
}
