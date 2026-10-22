package com.truenins.client.vfx;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.truenins.TrueNinsMod;
import com.truenins.client.domain.shingan.ShinganSceneEffect;
import com.truenins.client.orb.OrbVfxRenderer;
import com.truenins.voiddomain.client.ForgeWorldView;
import com.truenins.voiddomain.client.GlState;
import com.truenins.voiddomain.client.WaterSceneCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

/** Shared reference-style scene sampling, matrices, composition and GL state ownership. */
@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT)
public final class WorldVfxRenderer {
    private static final List<BiFunction<Entity, Float, WorldEffect>> PROVIDERS = new ArrayList<>();
    static {
        register(OrbVfxRenderer::effect);
        register(ShinganSceneEffect::effect);
        register(com.truenins.client.domain.judgment.JudgmentSceneEffect::effect);
        register(com.truenins.client.domain.judgment.ExecutionerSwordEffect::effect);
    }
    /** Two colour-only offscreen targets used to hand a composite pass to the next one. */
    private static final TextureTarget[] CHAIN = new TextureTarget[2];
    private WorldVfxRenderer() {}
    /** Register once during client setup. Return null for unrelated entities. */
    public static void register(BiFunction<Entity, Float, WorldEffect> provider) { PROVIDERS.add(provider); }
    public record Frame(float partial, float seconds, Vec3 camera, Matrix4f viewProjection,
                        Matrix4f inverseViewProjection, float projectionY, int width, int height,
                        Matrix4f view, Matrix3f normal, Matrix4f projection) {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        List<WorldEffect> effects = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) for (var provider : PROVIDERS) {
            WorldEffect effect = provider.apply(entity, event.getPartialTick());
            if (effect != null && effect.shader() != null) effects.add(effect);
        }
        com.truenins.client.conflagration.ConflagrationScene.collect(effects, event.getPartialTick());
        effects.removeIf(effect -> effect.shader() == null);
        if (effects.isEmpty()) return;
        var world = ForgeWorldView.finish(event);
        Matrix4f vp = new Matrix4f(world.getProjectionMatrix()).mul(world.getPoseStack().last().pose());
        Frame frame = new Frame(event.getPartialTick(), (mc.level.getGameTime() % 24000 + event.getPartialTick()) / 20f,
            event.getCamera().getPosition(), vp, new Matrix4f(vp).invert(), Math.abs(world.getProjectionMatrix().m11()),
            mc.getMainRenderTarget().width, mc.getMainRenderTarget().height,
            new Matrix4f(world.getPoseStack().last().pose()), new Matrix3f(world.getPoseStack().last().normal()),
            new Matrix4f(world.getProjectionMatrix()));
        effects.sort(Comparator.comparingInt(WorldEffect::layer).thenComparing(
            Comparator.comparingDouble((WorldEffect e) -> e.center().distanceToSqr(frame.camera())).reversed()));
        try (GlState.State ignored = new GlState.State()) {
            // A "replace" pass (composite, no world occlusion) only needs the *previous* colour, so
            // the whole run can share one world capture and ping-pong through offscreen targets
            // instead of blitting the full framebuffer once per effect.
            // One world capture per frame: the chain reuses it, and the fallback path reuses it
            // for its first pass instead of blitting the framebuffer twice in the same frame.
            if (!WaterSceneCapture.capture()) return;
            if (chainable(effects)) {
                drawChained(mc, effects, frame);
            } else {
                drawEachPass(effects, frame, true);
            }
        }
    }

    private static boolean chainable(List<WorldEffect> effects) {
        if (effects.size() < 2) return false;
        for (WorldEffect effect : effects) if (!effect.composite() || effect.occludesWorld()) return false;
        return true;
    }

    /** Draws every pass through one offscreen hand-off; the last pass writes into the main target. */
    private static void drawChained(Minecraft mc, List<WorldEffect> effects, Frame frame) {
        int width = frame.width();
        int height = frame.height();
        int last = effects.size() - 1;
        // Seed the first target with this frame: a pass whose alpha is still fading in (Visibility < 1)
        // must composite over the current scene, not over the previous frame left in the target.
        TextureTarget seed = target(0, width, height);
        seed.clear(Minecraft.ON_OSX);
        WaterSceneCapture.seed(seed);
        for (int i = 0; i < effects.size(); i++) {
            TextureTarget destination = i == last ? null : target(i & 1, width, height);
            if (destination == null) { mc.getMainRenderTarget().bindWrite(true); }
            else { destination.bindWrite(true); }
            int sceneColor = i == 0
                ? WaterSceneCapture.color()
                : target((i - 1) & 1, width, height).getColorTextureId();
            drawPass(effects.get(i), sceneColor, WaterSceneCapture.depth(), frame);
        }
        mc.getMainRenderTarget().bindWrite(true);
    }

    /** Legacy path: one world capture per pass, used whenever a pass cannot chain. */
    private static void drawEachPass(List<WorldEffect> effects, Frame frame, boolean firstCaptured) {
        boolean captured = firstCaptured;
        for (WorldEffect effect : effects) {
            // Composites sample all earlier passes, preventing one spell from erasing another.
            if (!captured && !WaterSceneCapture.capture()) break;
            captured = false;
            drawPass(effect, WaterSceneCapture.color(), WaterSceneCapture.depth(), frame);
        }
    }

    private static TextureTarget target(int slot, int width, int height) {
        TextureTarget target = CHAIN[slot];
        if (target == null || target.width != width || target.height != height) {
            if (target != null) target.destroyBuffers();
            target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            target.setClearColor(0f, 0f, 0f, 0f);
            CHAIN[slot] = target;
        }
        return target;
    }

    private static void drawPass(WorldEffect effect, int sceneColor, int sceneDepth, Frame frame) {
        ShaderInstance shader = effect.shader();
        shader.setSampler("SceneDepth", sceneDepth);
        shader.setSampler("SceneColor", sceneColor);
        shader.safeGetUniform("InverseViewProjection").set(frame.inverseViewProjection());
        shader.safeGetUniform("ViewProjection").set(frame.viewProjection());
        shader.safeGetUniform("Time").set(frame.seconds());
        shader.safeGetUniform("ScreenSize").set((float)frame.width(), (float)frame.height());
        vector(shader, "CenterRelative", effect.center().subtract(frame.camera()));
        shader.safeGetUniform("Visibility").set(1f);
        effect.uniforms().bind(shader, frame);
        if (effect.occludesWorld()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(519); // ALWAYS; the material writes min(world, barrier) depth.
        } else {
            RenderSystem.disableDepthTest(); // Volumes compare sampled world / barrier depth.
        }
        RenderSystem.depthMask(effect.occludesWorld());
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        // Volumes return premultiplied colour: never multiply alpha twice.
        RenderSystem.blendFunc(1, effect.composite() ? 0 : 771);
        RenderSystem.setShader(() -> shader);
        quad();
    }

    public static void vector(ShaderInstance shader, String name, Vec3 value) {
        shader.safeGetUniform(name).set((float)value.x, (float)value.y, (float)value.z);
    }
    private static void quad() {
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        b.vertex(-1, -1, 1).uv(0, 0).color(-1).endVertex();
        b.vertex(1, -1, 1).uv(1, 0).color(-1).endVertex();
        b.vertex(1, 1, 1).uv(1, 1).color(-1).endVertex();
        b.vertex(-1, 1, 1).uv(0, 1).color(-1).endVertex();
        BufferUploader.drawWithShader(b.end());
    }
}
