package com.truenins.client.domain.judgment;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.truenins.entity.domain.JudgmentDomainEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static com.truenins.client.render.PixelGeometry.*;
import static com.truenins.entity.domain.JudgmentDomainEntity.smooth;

/** Actual textured Minecraft geometry, staged radially. Never writes or replaces world blocks. */
public final class JudgmentDomainRenderer extends EntityRenderer<JudgmentDomainEntity> {
    private record Tile(int x, int z, double radius, BlockState state, double baseY, float start, float duration) {}
    private static final List<Tile> FLOOR = new ArrayList<>();
    private static final ResourceLocation WOOD = new ResourceLocation("minecraft", "textures/block/spruce_planks.png");
    private static final ResourceLocation ROBE = new ResourceLocation("truenins", "textures/effect/judgment_robe.png");
    static {
        int radius = (int)JudgmentDomainEntity.COURT_RADIUS;
        for (int x = -radius; x < radius; x++) for (int z = -radius; z < radius; z++) {
            double r = Math.hypot(x + .5, z + .5);
            if (r >= radius) continue;
            boolean inner = r < JudgmentDomainEntity.INNER_RADIUS;
            boolean step = !inner && r < JudgmentDomainEntity.INNER_RADIUS + 1;
            BlockState state;
            if (inner) state = r < 2.1 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
                : Blocks.STONE_BRICKS.defaultBlockState();
            else if (step) {
                Direction uphill = Math.abs(x + .5) > Math.abs(z + .5)
                    ? (x < 0 ? Direction.WEST : Direction.EAST)
                    : (z < 0 ? Direction.NORTH : Direction.SOUTH);
                state = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, uphill);
            } else state = r >= 20 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState();
            float start = inner ? 24 + (float)r * 2.1f : step ? 66 : 82 + (float)(r - 12) * 1.6f;
            FLOOR.add(new Tile(x, z, r, state, inner ? -.99 : .01, start, inner ? 19 : 18));
        }
    }
    public JudgmentDomainRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0; }
    @Override public ResourceLocation getTextureLocation(JudgmentDomainEntity d) { return PIXEL; }
    private static float appear(float age, float start, float duration, float close, double radius) {
        return smooth((age - start) / duration) * (1 - smooth((close * 1.4f - (float)(1 - radius / JudgmentDomainEntity.COURT_RADIUS) * .4f)));
    }
    @Override public void render(JudgmentDomainEntity d, float yaw, float partial, PoseStack p, MultiBufferSource buffers, int light) {
        if (!JudgmentStageCapture.capturing()) return;
        float age = d.visualAge(partial), close = d.closingProgress(partial);
        if (age < 24 || close >= 1) return;
        p.pushPose();
        p.mulPose(Axis.YP.rotationDegrees(d.getYRot()));
        // Small stone circle, a full-height stair ring, then a raised wooden annulus.
        // Grid-aligned tiles join without the gaps/overlap of a ring of rotated blocks.
        for (Tile tile : FLOOR) {
            float rise = appear(age, tile.start, tile.duration, close, tile.radius);
            if (rise <= .001f) continue;
            block(p, buffers, tile.state, tile.x, tile.baseY - (1 - rise) * 1.8, tile.z, 1, 1, 1);
        }
        // Twenty-four columns stand on the stone rim of the large wooden circle.
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            float f = appear(age, 100 + i % 4 * 2, 26, close, 20.4);
            if (f <= .001f) continue;
            p.pushPose(); p.mulPose(Axis.YP.rotation((float)a));
            block(p,buffers,Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),-.75,1.01-(1-f),19.65,1.5,.6,1.5);
            block(p,buffers,Blocks.STONE_BRICKS.defaultBlockState(),-.48,1.61,19.92,.96,7.6*f,.96);
            block(p,buffers,Blocks.POLISHED_ANDESITE.defaultBlockState(),-.72,1.61+7.6*f,19.68,1.44,.5,1.44);
            block(p,buffers,Blocks.STONE_BRICK_WALL.defaultBlockState(),-.22,2.11+7.6*f,20.18,.44,.6,.44);
            p.popPose();
        }
        // Custom curved witness rails, two opposed stands; the centre-facing doorway stays open.
        for (int side : new int[]{-1,1}) {
            float f = appear(age, 68, 32, close, 4);
            if (f > .001f) {
                p.pushPose(); p.translate(0,-(1-f)*1.8,side*4);
                railing(p,buffers,side,f); p.popPose();
            }
        }
        float judge = appear(age, 86, 46, close, 9);
        if (judge > .001f) {
            p.pushPose(); p.translate(0, -(1-judge)*5, 9.4);
            block(p,buffers,Blocks.DEEPSLATE_BRICKS.defaultBlockState(),-2,0,-.7,4,1.5,2);
            block(p,buffers,Blocks.SPRUCE_PLANKS.defaultBlockState(),-2.2,1.5,-.9,4.4,.3,2.4);
            judge(p,buffers,age,judge); p.popPose();
        }
        lamps(p,buffers,age,close);
        // The sword is a depth-aware luminous volume in ExecutionerSwordEffect.
        p.popPose();
    }
    /** One baked template per state instead of a full block-renderer pass per block. */
    private static void block(PoseStack p, MultiBufferSource buffers, BlockState state,
                               double x,double y,double z,double sx,double sy,double sz) {
        BlockMeshCache.emit(p, buffers, state, x, y, z, sx, sy, sz);
    }
    private static void railing(PoseStack p, MultiBufferSource buffers,int side,float alpha) {
        VertexConsumer wood = buffers.getBuffer(RenderType.entityCutoutNoCull(WOOD));
        for (int i=0;i<24;i++) {
            double a=i*Math.PI*2/24, b=(i+1)*Math.PI*2/24;
            double z=Math.cos(a)*1.5;
            if (z*side < -1.12) continue;
            Vec3 from=new Vec3(Math.sin(a)*1.5,1.12,z), to=new Vec3(Math.sin(b)*1.5,1.12,Math.cos(b)*1.5);
            beam(p,wood,from,to,.2,0xB09476,alpha,FULL_BRIGHT);
            beam(p,wood,from.add(0,-.65,0),to.add(0,-.65,0),.11,0x866950,alpha,FULL_BRIGHT);
            box(p,wood,from.x-.065,0,from.z-.065,.13,1.15,.13,0xAE9270,alpha,FULL_BRIGHT);
        }
        box(p,wood,-.85,.9,side*.92-.25,1.7,.18,.5,0xAD8F68,alpha,FULL_BRIGHT);
    }
    private static void judge(PoseStack p, MultiBufferSource buffers,float age,float alpha) {
        p.pushPose(); p.translate(0,2.5+Math.sin(age*.045)*.06,0);
        VertexConsumer robe=buffers.getBuffer(RenderType.entityCutoutNoCull(ROBE));
        // The reference silhouette is a hooded, uneven shadow rather than a mannequin:
        // stepped shoulders, a pinched waist and split hanging cloth keep the outline
        // readable from either witness stand while staying within Minecraft geometry.
        // ROBE is already a dark charcoal texture; multiplying it by another near-black
        // tint erased every fold in the independently lit stage.
        int shadow = 0x8390A4, shadowMid = 0xACB7C8, shadowEdge = 0xDEE3F0;
        cloak(p,robe,-.45,1.38,.55,1.30,.76,shadowMid,alpha);
        cloak(p,robe,.92,2.12,.38,.48,.70,shadow,alpha);
        // Angled shoulder folds break the rectangular top edge.
        p.pushPose(); p.translate(-1.02,.75,-.03); p.mulPose(Axis.ZP.rotationDegrees(-17));
        cloak(p,robe,-.30,.48,.28,.60,.66,shadowEdge,alpha); p.popPose();
        p.pushPose(); p.translate(1.02,.75,-.03); p.mulPose(Axis.ZP.rotationDegrees(14));
        cloak(p,robe,-.38,.48,.24,.60,.66,shadowEdge,alpha); p.popPose();
        // Two long, separated tails and a shorter torn centre tail.
        p.pushPose(); p.translate(-.55,-.55,.02); p.mulPose(Axis.ZP.rotationDegrees(-7));
        cloak(p,robe,-.72,.76,.16,.38,.58,shadow,alpha); p.popPose();
        p.pushPose(); p.translate(.55,-.55,.02); p.mulPose(Axis.ZP.rotationDegrees(7));
        cloak(p,robe,-.72,.76,.12,.38,.58,shadow,alpha); p.popPose();
        box(p,robe,-.22,-.38,-.33,.44,.86,.64,shadowMid,alpha,FULL_BRIGHT);
        box(p,robe,-.82,-1.25,-.27,.46,.42,.52,shadowEdge,alpha,FULL_BRIGHT);
        box(p,robe,.36,-1.25,-.27,.46,.42,.52,shadowEdge,alpha,FULL_BRIGHT);
        // Short square tendrils make the lower silhouette irregular without spawning
        // dozens of particles every frame.
        beam(p,robe,new Vec3(-.95,.2,.02),new Vec3(-1.42,-.65,.02),.22,shadow,alpha,FULL_BRIGHT);
        beam(p,robe,new Vec3(.95,.2,.02),new Vec3(1.42,-.56,.02),.22,shadow,alpha,FULL_BRIGHT);
        beam(p,robe,new Vec3(-.72,-.9,.02),new Vec3(-.96,-1.55,.02),.16,shadowMid,alpha,FULL_BRIGHT);
        beam(p,robe,new Vec3(.72,-.9,.02),new Vec3(.98,-1.48,.02),.16,shadowMid,alpha,FULL_BRIGHT);
        // Solid mask/scales must write depth: the scene pass otherwise mistakes them for the black vault.
        VertexConsumer pixel=buffers.getBuffer(RenderType.entityCutoutNoCull(PIXEL));
        int mask = 0xE7E0D2;
        box(p,pixel,-.25,1.20,-.38,.50,.18,.12,0xBCB4AE,alpha,FULL_BRIGHT);
        box(p,pixel,-.34,1.37,-.38,.68,.53,.12,mask,alpha,FULL_BRIGHT);
        box(p,pixel,-.27,1.90,-.38,.54,.18,.12,0xD6CEC5,alpha,FULL_BRIGHT);
        // Narrow eye slits and a lower seam preserve the mask's distinctive expression.
        box(p,pixel,-.26,1.64,-.445,.18,.055,.025,0x171621,alpha,FULL_BRIGHT);
        box(p,pixel,.08,1.64,-.445,.18,.055,.025,0x171621,alpha,FULL_BRIGHT);
        box(p,pixel,-.03,1.45,-.447,.06,.16,.025,0x39313C,alpha,FULL_BRIGHT);
        box(p,pixel,-.12,1.38,-.447,.24,.035,.025,0x39313C,alpha,FULL_BRIGHT);
        for(int i=0;i<3;i++) for(int s:new int[]{-1,1})
            box(p,pixel,s*.18+i*.045-.045,1.56,-.45,.022,.10,.022,0x5D5360,alpha,FULL_BRIGHT);
        // Copper scales suspended from the sleeve, recognizable from either witness stand.
        beam(p,pixel,new Vec3(-1.57,.6,-.1),new Vec3(-1.57,-.8,-.1),.045,0xC5A15E,alpha,FULL_BRIGHT);
        beam(p,pixel,new Vec3(-2.15,-.35,-.1),new Vec3(-1,-.35,-.1),.05,0xC5A15E,alpha,FULL_BRIGHT);
        for(double x:new double[]{-2.13,-1.02}) {
            beam(p,pixel,new Vec3(x,-.35,-.1),new Vec3(x,-1.03,-.1),.025,0xC5A15E,alpha,FULL_BRIGHT);
            box(p,pixel,x-.22,-1.08,-.3,.44,.06,.4,0xB28B4C,alpha,FULL_BRIGHT);
        }
        p.popPose();
        // The gavel settles after each audible strike instead of hanging motionless.
        float strike = JudgmentTimeline.gavelAngle(age,132)+JudgmentTimeline.gavelAngle(age,170);
        p.pushPose(); p.translate(1.3,1.9,-.4); p.mulPose(Axis.XP.rotationDegrees(strike));
        VertexConsumer wood=buffers.getBuffer(RenderType.entityCutoutNoCull(WOOD));
        box(p,wood,-.05,0,-.05,.1,.7,.1,0xA47E50,alpha,FULL_BRIGHT);
        box(p,wood,-.32,.58,-.16,.64,.28,.32,0xAF8E62,alpha,FULL_BRIGHT);
        p.popPose();
    }
    /** Six solid faces make a tapered cloth fold without a stack of rectangular slabs. */
    private static void cloak(PoseStack p,VertexConsumer out,double bottom,double top,
                              double lowerWidth,double upperWidth,double depth,int color,float alpha) {
        Vec3 a=new Vec3(-lowerWidth,bottom,-depth/2),b=new Vec3(lowerWidth,bottom,-depth/2);
        Vec3 c=new Vec3(upperWidth,top,-depth/2),d=new Vec3(-upperWidth,top,-depth/2);
        Vec3 e=a.add(0,0,depth),f=b.add(0,0,depth),g=c.add(0,0,depth),h=d.add(0,0,depth);
        face(p,out,b,a,d,c,color,alpha,FULL_BRIGHT);
        face(p,out,e,f,g,h,shade(color,.85f),alpha,FULL_BRIGHT);
        face(p,out,a,e,h,d,shade(color,.72f),alpha,FULL_BRIGHT);
        face(p,out,f,b,c,g,shade(color,.9f),alpha,FULL_BRIGHT);
        face(p,out,d,h,g,c,color,alpha,FULL_BRIGHT);
        face(p,out,a,b,f,e,shade(color,.62f),alpha,FULL_BRIGHT);
    }
    private static void lamps(PoseStack p,MultiBufferSource buffers,float age,float close) {
        float f=appear(age,86,34,close,18.6);
        if(f<=.001f)return;
        boolean warm=!JudgmentTimeline.cold(age);
        int color=warm?0xFFCC76:0x95C9FF;
        VertexConsumer pixel=buffers.getBuffer(RenderType.entityCutoutNoCull(PIXEL));
        for(int i=0;i<12;i++) {
            double a=i*Math.PI/6, x=Math.sin(a)*18.6,z=Math.cos(a)*18.6;
            p.pushPose();p.translate(0,2,0);
            box(p,pixel,x-.14,5.9*f,z-.14,.28,.48,.28,color,f,FULL_BRIGHT);
            box(p,pixel,x-.22,5.86*f,z-.22,.44,.08,.44,0x38323C,f,FULL_BRIGHT);
            box(p,pixel,x-.22,6.36*f,z-.22,.44,.08,.44,0x38323C,f,FULL_BRIGHT);
            beam(p,pixel,new Vec3(x,6.4*f,z),new Vec3(x,7.15*f,z),.045,0x38323C,f,FULL_BRIGHT);
            p.popPose();
        }
        pixel=buffers.getBuffer(RenderType.entityTranslucentEmissive(PIXEL));
        for(int i=0;i<12;i++) {
            double a=i*Math.PI/6, x=Math.sin(a)*18.6,z=Math.cos(a)*18.6;
            for(int j=0;j<3;j++)box(p,pixel,x-.20-j*.10,2+5.86*f-j*.05,z-.20-j*.10,
                .4+j*.2,.56+j*.10,.4+j*.2,color,.035f*f,FULL_BRIGHT);
        }
    }
    /** Close shot through the same stage target; contact is exactly at the entrance sound cue. */
    public static void entrance(float age, PoseStack p, MultiBufferSource buffers) {
        float fade=1-smooth((age-20)/4);
        if(fade<=0)return;
        VertexConsumer wood=buffers.getBuffer(RenderType.entityCutoutNoCull(WOOD));
        p.pushPose(); p.translate(.15,-.58,-3.1); p.mulPose(Axis.XP.rotationDegrees(18));
        box(p,wood,-2,-.35,-1.4,4,.25,2.8,0x967350,fade,FULL_BRIGHT);
        box(p,wood,-.53,-.10,-.50,1.06,.10,1.0,0xC29460,fade,FULL_BRIGHT);
        p.translate(.65,.25,0); p.mulPose(Axis.ZP.rotationDegrees(JudgmentTimeline.gavelAngle(age,JudgmentTimeline.ENTRANCE_HIT)));
        box(p,wood,-.68,-.03,-.055,1.20,.11,.11,0xB88C59,fade,FULL_BRIGHT);
        box(p,wood,-.91,-.23,-.23,.40,.40,.46,0xBF9463,fade,FULL_BRIGHT);
        box(p,wood,-.94,-.25,-.25,.08,.44,.50,0xD1A874,fade,FULL_BRIGHT);
        box(p,wood,-.56,-.25,-.25,.08,.44,.50,0xD1A874,fade,FULL_BRIGHT);
        p.popPose();
    }
}