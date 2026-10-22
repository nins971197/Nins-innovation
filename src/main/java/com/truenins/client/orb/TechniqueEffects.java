package com.truenins.client.orb;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.truenins.TrueNinsConfig;
import com.truenins.client.render.PixelGeometry;
import com.truenins.client.render.PixelRenderTypes;
import com.truenins.entity.orb.TNOrbEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static com.truenins.client.render.PixelGeometry.*;

/** Animated volumes and angular strokes, without camera-facing sphere photographs. */
public final class TechniqueEffects {
    private static final Vec3 X = new Vec3(1,0,0), Y = new Vec3(0,1,0), Z = new Vec3(0,0,1);
    private TechniqueEffects() {}

    public static void blue(PoseStack pose, MultiBufferSource buffers, float time) {
        VertexConsumer out = buffers.getBuffer(PixelRenderTypes.volume(PIXEL));
        float beat = 1 + .10f * Mth.sin(time * .39f) + .035f * Mth.sin(time * 1.7f);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation(time * -.073f));
        pose.mulPose(Axis.ZP.rotation(.4f + time * .031f));
        core(pose, out, .42 * beat, 0xE9FBFF, 0x1763B5);
        for (int layer = 0; layer < 3; layer++) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotation(layer * .94f + time * .014f));
            arc(pose, out, X, Z, .68 + layer * .19, time * (layer == 1 ? -.19 : .15) + layer * 2,
                4.3, .038 + layer * .005, layer == 1 ? 0xB4F6FF : 0x238EFF, .86f, 13);
            pose.popPose();
        }
        pose.popPose();
        // Five independently phased filaments visibly travel inward and wind around the core.
        for (int arm = 0; arm < 5; arm++) {
            double phase = fract(time * .045 + arm * .2);
            Vec3 prev = null;
            for (int j = 0; j <= 7; j++) {
                double tail = j / 7.0;
                double radius = .38 + (1 - phase) * 2.0 + tail * .65;
                double angle = arm * 2.39996 + time * .11 + tail * .9;
                Vec3 p = new Vec3(Math.cos(angle)*radius,
                    Math.sin(angle * .7 + arm)*radius*.48, Math.sin(angle)*radius);
                if (prev != null && j != 4) beam(pose,out,prev,p,.025 + .018*(1-tail),
                    j < 3 ? 0xBDF7FF : 0x2884DE, (float)((1-tail*.7)*.62),FULL_BRIGHT);
                prev = p;
            }
            cube(pose,out,prev,.075,0x63BDFF,.7f);
        }
        for (int i=0;i<14;i++) {
            double phase = fract(time*.055+i*.618034);
            double radius = .48 + (1-phase)*1.75;
            double angle = i*2.39996 + time*.07;
            cube(pose,out,new Vec3(Math.cos(angle)*radius,Math.sin(i*1.9+time*.08)*radius*.65,
                Math.sin(angle)*radius), .035+.065*phase, i%4==0?0xD4FAFF:0x2D8EEB,(float)(.25+.7*phase));
        }
    }

    public static void red(PoseStack pose, MultiBufferSource buffers, float time, Vec3 direction,
                            boolean flying, float explosion) {
        VertexConsumer out = buffers.getBuffer(PixelRenderTypes.volume(PIXEL));
        Vec3 axis = direction.normalize();
        Vec3 side = axis.cross(Math.abs(axis.y)>.95?X:Y).normalize();
        Vec3 up = side.cross(axis).normalize();
        if (explosion >= 0) {
            burst(pose,out,time,explosion,axis,side,up);
            return;
        }
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation(time*.16f));
        pose.mulPose(Axis.XP.rotation(time*-.11f));
        double pulse = 1 + .13*Math.sin(time*.65);
        core(pose,out,.20*pulse,0xFFF0D7,0xEF2641);
        pose.popPose();
        for(int ring=0;ring<3;ring++) {
            double phase=fract(time*.065 + ring/3.0);
            double radius=flying?.32+phase*.95:.75-phase*.42;
            Vec3 center=axis.scale(flying?-phase*2.8:Math.sin(time*.2+ring)*.12);
            pose.pushPose();
            pose.translate(center.x,center.y,center.z);
            arc(pose,out,side,up,radius,time*-.23+ring*2.1,4.7,.045,
                ring==0?0xFFD6C5:0xF92B42,(float)(.86*(1-phase)),12);
            pose.popPose();
        }
        for (int arm=0;arm<6;arm++) {
            double angle=arm*Math.PI/3 + Math.floor(time/2)*.19;
            Vec3 radial=side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            double extension=.8+.24*Math.sin(time*.5+arm*2);
            Vec3 from=radial.scale(.25);
            Vec3 elbow=radial.scale(extension*.7).add(axis.scale(flying?-.8:.05));
            Vec3 tip=radial.scale(extension).add(axis.scale(flying?-2.9:.16));
            beam(pose,out,from,elbow,.038,0xFFCCD0,.83f,FULL_BRIGHT);
            beam(pose,out,elbow,tip,.027,0xB80E35,.65f,FULL_BRIGHT);
        }
        if (flying) for(int i=0;i<14;i++) {
            double t=fract(time*.075+i*.618034);
            double angle=i*2.4+time*.13;
            Vec3 p=axis.scale(-t*4).add(side.scale(Math.cos(angle)*t*.55)).add(up.scale(Math.sin(angle)*t*.55));
            cube(pose,out,p,.055+.05*(1-t),i%3==0?0xFFE1C9:0xE22241,(float)(1-t));
        }
    }

    public static void purple(PoseStack pose, MultiBufferSource buffers, float time, int phase, int explosion) {
        VertexConsumer out = buffers.getBuffer(PixelRenderTypes.volume(PIXEL));
        if (explosion >= 0) {
            float t = Mth.clamp(explosion / (float)TNOrbEntity.EXPLODE_TICKS, 0, 1);
            double radius = .5 + TrueNinsConfig.purpleBlastRadius() * Math.pow(t, .55);
            float fade = (1 - t) * (1 - t);
            core(pose, out, .72 * (1 - .5 * t), 0xF7EEFF, 0xA72BFF);
            arc(pose, out, X, Z, radius, time * .03, 6.1, .10, 0xD66BFF, fade, 42);
            arc(pose, out, Y, X, radius * .82, time * -.02, 6.1, .08, 0x6B4CFF, fade * .8f, 36);
            for (int i = 0; i < 42; i++) {
                double a = i * 2.39996 + time * .04, y = 1 - 2 * (i + .5) / 42;
                Vec3 d = new Vec3(Math.cos(a) * Math.sqrt(1-y*y), y, Math.sin(a) * Math.sqrt(1-y*y));
                beam(pose, out, d.scale(radius * .6), d.scale(radius * 1.18), .045, i % 2 == 0 ? 0xE8C6FF : 0x7B65FF, fade, FULL_BRIGHT);
            }
            // Short, broken arcs keep the blast irregular like the reference footage while
            // retaining stepped Minecraft geometry instead of a smooth photographic shell.
            for (int i = 0; i < 16; i++) {
                double a = i * 2.39996 + time * .09;
                Vec3 radial = new Vec3(Math.cos(a), .18 * Math.sin(a * 1.7), Math.sin(a)).normalize();
                Vec3 tangent = radial.cross(Math.abs(radial.y) > .9 ? X : Y).normalize();
                Vec3 from = radial.scale(radius * (.72 + .06 * Math.sin(time * .2 + i)));
                Vec3 previous = from;
                for (int segment = 1; segment <= 4; segment++) {
                    double along = segment / 4.0;
                    Vec3 next = radial.scale(radius * (.72 + .5 * along))
                        .add(tangent.scale(Math.sin(time * .11 + i * 1.7 + segment * 2.1) * radius * .11));
                    beam(pose, out, previous, next, .035, i % 3 == 0 ? 0xF5D9FF : 0xB884FF,
                        fade * .9f, FULL_BRIGHT);
                    previous = next;
                }
            }
            return;
        }
        float f = Mth.clamp(phase / (float)Math.max(1, TrueNinsConfig.purpleFusionTicks()), 0, 1);
        double orbit = 1.8 * (1 - f) + .12;
        for (int i = 0; i < 2; i++) {
            double a = time * (i == 0 ? .08 : -.1) + i * Math.PI;
            Vec3 c = new Vec3(Math.cos(a) * orbit, Math.sin(a * 1.3) * orbit * .5, Math.sin(a) * orbit);
            pose.pushPose(); pose.translate(c.x, c.y, c.z);
            core(pose, out, .24 + .22 * f, i == 0 ? 0x092C73 : 0x7C102B, i == 0 ? 0x46BFFF : 0xFF6A72);
            pose.popPose();
            beam(pose, out, c, Vec3.ZERO, .035, i == 0 ? 0x4AB9FF : 0xFF5268, .65f, FULL_BRIGHT);
        }
        core(pose, out, .14 + .56 * f, 0xF4E8FF, 0xA43DFF);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8 + time * .04;
            cube(pose, out, new Vec3(Math.cos(a) * (.55 + f), Math.sin(time * .08 + i) * .25, Math.sin(a) * (.55 + f)), .04 + .05 * f, 0xC56BFF, .55f + .4f * f);
        }
    }

    private static void burst(PoseStack p, VertexConsumer out, float time, float ticks,
                               Vec3 axis, Vec3 side, Vec3 up) {
        float t=Mth.clamp(ticks/TNOrbEntity.EXPLODE_TICKS,0,1);
        double radius=.35+TrueNinsConfig.heBlastRadius()*Math.pow(t,.42);
        float fade=(1-t)*(1-t);
        core(p,out,.15+.6*(1-t),0xFFF5E7,0xF02B46);
        arc(p,out,side,up,radius,0,Math.PI*2,.06+.13*(1-t),0xFF6671,fade,24);
        arc(p,out,side,axis,radius*.88,time*.02,Math.PI*2,.055,0xEF2346,fade*.75f,20);
        arc(p,out,X,Z,radius*1.13,time*.03,Math.PI*2,.07,0xFFD6C9,fade*.6f,24);
        for(int i=0;i<22;i++) {
            double y=1-2*(i+.5)/22, r=Math.sqrt(1-y*y), a=i*2.39996;
            Vec3 dir=new Vec3(Math.cos(a)*r,y,Math.sin(a)*r);
            beam(p,out,dir.scale(radius*.72),dir.scale(radius*(1.15+.1*Math.sin(i))),
                .035+.035*(1-t),i%4==0?0xFFF0D8:0xEA2449,fade,FULL_BRIGHT);
            cube(p,out,dir.scale(radius),.07+.08*(1-t),0xFF8791,fade);
        }
    }

    private static void core(PoseStack p,VertexConsumer out,double size,int center,int rim) {
        // The ball is a broken shell around a white-hot nucleus. Leaving gaps between
        // shell cells exposes the core and keeps the silhouette from becoming a flat
        // solid-color cube when viewed at a distance.
        int cells = 3;
        double cell = size * .27;
        for (int x = -cells; x <= cells; x++) for (int y = -cells; y <= cells; y++)
            for (int z = -cells; z <= cells; z++) {
                double d2 = x * x + y * y + z * z;
                if (d2 > 11.2) continue;
                boolean shell = d2 >= 5.5;
                if (!shell && d2 > 2.5) continue;
                double edge = Math.min(1.0, Math.sqrt(d2) / 3.35);
                int color = shell ? (edge < .88 ? rim : shade(rim, .58f)) : center;
                float alpha = shell ? (float)(.62 + edge * .18) : .98f;
                cube(p, out, new Vec3(x * cell, y * cell, z * cell), cell * (shell ? .76 : 1.08), color, alpha);
            }
        cube(p, out, Vec3.ZERO, cell * .74, center, 1.0f);
        // Fine, independently phased loops give the shell a visible material response.
        arc(p, out, X, Y, size * .78, .10, 5.25, size * .035, shade(rim, .82f), .68f, 15);
        arc(p, out, Y, Z, size * .84, .72, 4.55, size * .026, rim, .54f, 13);
        arc(p, out, Z, X, size * .91, 1.35, 3.85, size * .022, shade(rim, .48f), .42f, 11);
    }

    public static void arc(PoseStack p,VertexConsumer out,Vec3 u,Vec3 v,double radius,double start,
                           double length,double width,int color,float alpha,int segments) {
        for(int i=0;i<segments;i++) {
            if(i%7==5) continue;
            double a=start+length*i/segments, b=start+length*(i+1)/segments;
            beam(p,out,u.scale(Math.cos(a)*radius).add(v.scale(Math.sin(a)*radius)),
                u.scale(Math.cos(b)*radius).add(v.scale(Math.sin(b)*radius)),width,color,alpha,FULL_BRIGHT);
        }
    }
    private static double fract(double n) { return n-Math.floor(n); }
}
