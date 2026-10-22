package com.truenins.client.domain.judgment;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.truenins.client.vfx.WorldVfxRenderer.Frame;
import com.truenins.entity.domain.JudgmentDomainEntity;
import com.truenins.voiddomain.client.GlState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** A separate court and actor depth buffer: terrain is never erased or replaced in the world. */
public final class JudgmentStageCapture {
    private static RenderTarget target;
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(1 << 20));
    private static boolean capturing;
    private record Actors(long tick, List<LivingEntity> entities) {}
    private static final Map<JudgmentDomainEntity, Actors> ACTORS = new WeakHashMap<>();
    private JudgmentStageCapture() {}
    public static boolean capturing() { return capturing; }
    public static int color() { return target.getColorTextureId(); }
    public static int depth() { return target.getDepthTextureId(); }

    public static void capture(JudgmentDomainEntity domain, Frame frame) {
        Minecraft mc = Minecraft.getInstance();
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4]; GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        float[] clear = new float[4]; GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clear);
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try (GlState.State ignored = new GlState.State()) {
            if (target == null || target.width != frame.width() || target.height != frame.height()) {
                if (target != null) target.destroyBuffers();
                target = new TextureTarget(frame.width(), frame.height(), true, Minecraft.ON_OSX);
                target.setClearColor(0, 0, 0, 0);
            }
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            // Outside observers only see the opaque shell; building its hidden interior
            // would submit the entire floor and all actors for no visible result.
            if (frame.camera().distanceToSqr(domain.position()) >= JudgmentDomainEntity.FIELD_RADIUS * JudgmentDomainEntity.FIELD_RADIUS) return;
            RenderSystem.enableDepthTest(); RenderSystem.depthFunc(GL11.GL_LEQUAL); RenderSystem.depthMask(true);
            modelView.setIdentity(); RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(frame.projection(), com.mojang.blaze3d.vertex.VertexSorting.DISTANCE_TO_ORIGIN);
            PoseStack poses = new PoseStack();
            poses.last().pose().set(frame.view()); poses.last().normal().set(frame.normal());
            capturing = true;
            var dispatcher = mc.getEntityRenderDispatcher();
            var relative = domain.position().subtract(frame.camera());
            dispatcher.render(domain, relative.x, relative.y, relative.z, domain.getYRot(), frame.partial(), poses, BUFFERS, 0xF000F0);
            if (domain.visualAge(frame.partial()) >= JudgmentDomainEntity.BLACKOUT_END) {
                for (LivingEntity entity : actors(domain)) {
                    if (!domain.affects(entity)) continue;
                    if (entity == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) continue;
                    var pos = new net.minecraft.world.phys.Vec3(Mth.lerp(frame.partial(), entity.xOld, entity.getX()),
                        Mth.lerp(frame.partial(), entity.yOld, entity.getY()), Mth.lerp(frame.partial(), entity.zOld, entity.getZ()))
                        .subtract(frame.camera());
                    dispatcher.render(entity, pos.x, pos.y, pos.z, Mth.rotLerp(frame.partial(), entity.yRotO, entity.getYRot()),
                        frame.partial(), poses, BUFFERS, 0xF000F0);
                }
            }
            BUFFERS.endBatch();
            if (domain.visualAge(frame.partial()) < JudgmentDomainEntity.BLACKOUT_END) {
                JudgmentDomainRenderer.entrance(domain.visualAge(frame.partial()), new PoseStack(), BUFFERS);
                BUFFERS.endBatch();
            }
        } finally {
            capturing = false;
            modelView.popPose(); RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.clearColor(clear[0], clear[1], clear[2], clear[3]);
        }
    }
    private static List<LivingEntity> actors(JudgmentDomainEntity domain) {
        long tick = domain.level().getGameTime();
        Actors cached = ACTORS.get(domain);
        if (cached != null && cached.tick == tick) return cached.entities;
        double r = JudgmentDomainEntity.FIELD_RADIUS;
        var center = domain.position().add(0, 3, 0);
        List<LivingEntity> entities = new ArrayList<>(domain.level().getEntitiesOfClass(LivingEntity.class,
            new AABB(center.x-r,center.y-r,center.z-r,center.x+r,center.y+r,center.z+r), domain::affects));
        for (Entity participant : new Entity[]{domain.advocate(),domain.defendant()})
            if (participant instanceof LivingEntity living && !entities.contains(living)) entities.add(living);
        ACTORS.put(domain, new Actors(tick, entities));
        return entities;
    }
    public static void release() {
        ACTORS.clear();
        if (target != null) { target.destroyBuffers(); target = null; }
    }
}
