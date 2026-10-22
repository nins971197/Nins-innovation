package com.truenins.client.infinity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.truenins.gameplay.enchantment.TrueNinsEnchantments;
import com.truenins.TrueNinsMod;
import com.truenins.register.TNMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT)
public final class InfinityShieldRenderer {

    private static final ResourceLocation BARRIER_TEXTURE = new ResourceLocation("block/glass");
    private static final float PLANE = 0.95F;
    private static final float RING_RADIUS = 0.26F;
    private static final float RING_INNER = 0.86F;
    private static final int RINGS = 3;
    private static final int SEGMENTS = 48;
    /** Shield ring lifetime, in game ticks (900 ms at 20 tps). Wall clock is not used: it
     *  desyncs from the level when the game is paused or the tick rate changes. */
    private static final long FLASH_TICKS = 18L;
    private static final int MAX_IMPACTS = 12;

    /** Impacts are grouped per entity id so that one player's shield never renders another
     *  player's hits. Entries are dropped when the entity leaves the level. */
    private static final Map<Integer, List<Impact>> IMPACTS = new HashMap<>();

    private static float lastHealth = -1.0F;

    /** The level and game time the current rings were recorded against; a new level resets the clock. */
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;
    private static long lastGameTime;

    private record Impact(Vec3 dir, long time, float plane) {}

    private InfinityShieldRenderer() {}

    public static void flash(Vec3 from, AbstractClientPlayer player) {
        flash(from, player, PLANE);
    }

    public static void flash(Vec3 from, AbstractClientPlayer player, float plane) {
        if (player == null) return;
        Vec3 dir = from == null
            ? new Vec3(0.0D, 0.0D, 1.0D)
            : from.subtract(player.position());
        if (dir.lengthSqr() < 1.0E-6D) dir = new Vec3(0.0D, 0.0D, 1.0D);
        dir = dir.normalize();
        long now = player.level().getGameTime();
        List<Impact> impacts = IMPACTS.computeIfAbsent(player.getId(), id -> new ArrayList<>());
        impacts.removeIf(i -> now - i.time() > FLASH_TICKS);
        impacts.add(new Impact(dir, now, plane));
        while (impacts.size() > MAX_IMPACTS) impacts.remove(0);
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onLocalAttacked(LivingAttackEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getEntity() != mc.player || !infinityReady(mc.player)) return;
        if (!wearsInfinity(mc.player)) return;
        Entity source = event.getSource().getDirectEntity();
        if (source == null) source = event.getSource().getEntity();
        if (source == null) return;
        flash(source.position(), mc.player);
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onLocalHurt(LivingHurtEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getEntity() != mc.player || !infinityReady(mc.player)) return;
        if (!wearsInfinity(mc.player)) return;
        Entity source = event.getSource().getDirectEntity();
        if (source == null) source = event.getSource().getEntity();
        if (source == null) return;
        flash(source.position(), mc.player);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) return;
        if (!infinityReady(mc.player) || !wearsInfinity(mc.player)) {
            lastHealth = -1.0F;
            return;
        }
        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (lastHealth >= 0.0F && hp < lastHealth - 0.05F) {
            Entity attacker = mc.player.getLastHurtByMob();
            if (attacker == null) {
                double best = 16.0D;
                for (LivingEntity le : mc.level.getEntitiesOfClass(LivingEntity.class,
                        mc.player.getBoundingBox().inflate(4.0D))) {
                    if (le == mc.player) continue;
                    double d = le.distanceToSqr(mc.player);
                    if (d < best) { best = d; attacker = le; }
                }
            }
            flash(attacker == null ? null : attacker.position(), mc.player);
        }
        lastHealth = hp;

        for (Projectile proj : mc.level.getEntitiesOfClass(Projectile.class,
                mc.player.getBoundingBox().inflate(3.0D))) {
            if (proj.getOwner() == mc.player) continue;
            if (proj.onGround() || proj.getDeltaMovement().lengthSqr() < 0.001D) continue;
            if (proj.position().distanceTo(mc.player.position()) <= 2.6D) {
                flash(proj.position(), mc.player);
            }
        }

        for (Mob mob : mc.level.getEntitiesOfClass(Mob.class,
                mc.player.getBoundingBox().inflate(3.0D))) {
            if (mob.distanceTo(mc.player) > 3.0F) continue;
            if (mob.attackAnim <= 0.1F || mob.attackAnim > 0.4F) continue;
            flash(mob.position(), mc.player, 0.55F);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long now = mc.level.getGameTime();
        // Game time is per-level: a dimension change or a reload rewinds it, which would leave
        // rings with a negative age (fade > 1) pointing at the previous world.
        if (mc.level != lastLevel || now < lastGameTime) IMPACTS.clear();
        lastLevel = mc.level;
        lastGameTime = now;
        IMPACTS.keySet().removeIf(id -> mc.level.getEntity(id) == null);
        for (List<Impact> list : IMPACTS.values()) list.removeIf(i -> now - i.time() > FLASH_TICKS);
        IMPACTS.values().removeIf(List::isEmpty);
        if (IMPACTS.isEmpty()) return;

        TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(BARRIER_TEXTURE);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        Vec3 camera = event.getCamera().getPosition();
        float time = (float) (now % 1200L) / 20.0F;

        for (AbstractClientPlayer player : mc.level.players()) {
            if (!wearsInfinity(player)) continue;
            List<Impact> impacts = IMPACTS.get(player.getId());
            if (impacts == null || impacts.isEmpty()) continue;

            float partial = event.getPartialTick();
            double ix = net.minecraft.util.Mth.lerp(partial, player.xOld, player.getX());
            double iy = net.minecraft.util.Mth.lerp(partial, player.yOld, player.getY());
            double iz = net.minecraft.util.Mth.lerp(partial, player.zOld, player.getZ());

            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            pose.translate(ix - camera.x,
                           iy + player.getBbHeight() * 0.5D - camera.y,
                           iz - camera.z);
            Matrix4f m = pose.last().pose();

            for (Impact impact : impacts) {
                float fade = net.minecraft.util.Mth.clamp(
                    1.0F - (float) (now - impact.time()) / (float) FLASH_TICKS, 0.0F, 1.0F);
                if (fade <= 0.0F) continue;
                arc(vc, m, sprite, time, impact.dir(), fade * fade, impact.plane());
            }
            pose.popPose();
        }
        buffers.endBatch(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
    }

    private static void arc(VertexConsumer vc, Matrix4f m, TextureAtlasSprite sprite,
                            float time, Vec3 dir, float strength, float plane) {
        Vec3 n = dir.normalize();
        Vec3 ref = Math.abs(n.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = n.cross(ref).normalize();
        Vec3 up = right.cross(n).normalize();

        float expand = 1.0F + 0.22F * (1.0F - strength);
        float radius = RING_RADIUS * expand;

        double cx = n.x * plane;
        double cy = n.y * plane;
        double cz = n.z * plane;

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        float glow = 0.55F + 0.45F * (0.5F + 0.5F * (float) Math.sin(time * 8.0F));

        for (int ring = 0; ring < RINGS; ring++) {
            float r0 = RING_INNER + (1.0F - RING_INNER) * ring / RINGS;
            float r1 = RING_INNER + (1.0F - RING_INNER) * (ring + 1) / RINGS;
            float a0 = band(r0) * strength * glow;
            float a1 = band(r1) * strength * glow;
            if (a0 + a1 <= 0.004F) continue;

            for (int seg = 0; seg < SEGMENTS; seg++) {
                float t0 = (float) (2.0D * Math.PI * seg / SEGMENTS);
                float t1 = (float) (2.0D * Math.PI * (seg + 1) / SEGMENTS);
                float jitter0 = 1.0F + 0.035F * (float) Math.sin(t0 * 6.0F + time * 5.0F);
                float jitter1 = 1.0F + 0.035F * (float) Math.sin(t1 * 6.0F + time * 5.0F);

                double x00 = cx + (right.x * Math.cos(t0) + up.x * Math.sin(t0)) * radius * r0 * jitter0;
                double y00 = cy + (right.y * Math.cos(t0) + up.y * Math.sin(t0)) * radius * r0 * jitter0;
                double z00 = cz + (right.z * Math.cos(t0) + up.z * Math.sin(t0)) * radius * r0 * jitter0;
                double x01 = cx + (right.x * Math.cos(t1) + up.x * Math.sin(t1)) * radius * r0 * jitter1;
                double y01 = cy + (right.y * Math.cos(t1) + up.y * Math.sin(t1)) * radius * r0 * jitter1;
                double z01 = cz + (right.z * Math.cos(t1) + up.z * Math.sin(t1)) * radius * r0 * jitter1;
                double x10 = cx + (right.x * Math.cos(t0) + up.x * Math.sin(t0)) * radius * r1 * jitter0;
                double y10 = cy + (right.y * Math.cos(t0) + up.y * Math.sin(t0)) * radius * r1 * jitter0;
                double z10 = cz + (right.z * Math.cos(t0) + up.z * Math.sin(t0)) * radius * r1 * jitter0;
                double x11 = cx + (right.x * Math.cos(t1) + up.x * Math.sin(t1)) * radius * r1 * jitter1;
                double y11 = cy + (right.y * Math.cos(t1) + up.y * Math.sin(t1)) * radius * r1 * jitter1;
                double z11 = cz + (right.z * Math.cos(t1) + up.z * Math.sin(t1)) * radius * r1 * jitter1;

                vertex(vc, m, x00, y00, z00, u0, v1, a0);
                vertex(vc, m, x10, y10, z10, u1, v1, a1);
                vertex(vc, m, x11, y11, z11, u1, v0, a1);
                vertex(vc, m, x01, y01, z01, u0, v0, a0);

                vertex(vc, m, x01, y01, z01, u0, v0, a0);
                vertex(vc, m, x11, y11, z11, u1, v0, a1);
                vertex(vc, m, x10, y10, z10, u1, v1, a1);
                vertex(vc, m, x00, y00, z00, u0, v1, a0);
            }
        }
    }

    private static float band(float r) {
        float d = Math.abs(r - 0.93F) * 7.0F;
        float v = 1.0F - d;
        return v <= 0.0F ? 0.0F : v * v;
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, double x, double y, double z,
                               float u, float v, float alpha) {
        vc.vertex(m, (float) x, (float) y, (float) z)
          .color(0.55F, 0.95F, 1.0F, Math.max(0.0F, Math.min(1.0F, alpha)))
          .uv(u, v)
          .overlayCoords(OverlayTexture.NO_OVERLAY)
          .uv2(0x00F000F0)
          .normal(0.0F, 0.0F, 1.0F)
          .endVertex();
    }

    private static boolean wearsInfinity(AbstractClientPlayer player) {
        if (player.hasEffect(TNMobEffects.INFINITY_FIELD.get())) return true;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            if (stack.hasTag() && stack.getTag().getBoolean("infinity")) return true;
            if (TrueNinsEnchantments.levelOf(stack, "infinity") > 0) return true;
        }
        return false;
    }

    private static boolean infinityReady(AbstractClientPlayer player) {
        if (player.hasEffect(TNMobEffects.INFINITY_FIELD.get())) return true;
        return player.experienceLevel > 0;
    }
}