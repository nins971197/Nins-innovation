package com.truenins.voiddomain.client;

import com.truenins.TrueNinsMod;
import com.truenins.entity.domain.VoidDomainEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT)
public final class VoidDomainClient {

    private VoidDomainClient() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        ForgeWorldView.capture(event);
        RenderLevelStageEvent.Stage stage = event.getStage();

        if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
            DomainWorldCapture.beginFrame();
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            DomainWorldCapture.terrain(event);
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            DomainWorldCapture.entities();
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            UnlimitedVoid domain = VoidDomain.active();
            if (domain != null && UnlimitedVoidRenderer.ready()) {
                try (GlState.State ignored = new GlState.State();
                     SceneSnapshot.Target target = new SceneSnapshot.Target()) {
                    UnlimitedVoidRenderer.render(ForgeWorldView.finish(event), domain);
                }
                VoidDump.maybeDump(ClientEffectClock.ticks(), Minecraft.getInstance());
            }
        }
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        UnlimitedVoid domain = VoidDomain.active();
        if (domain == null) return;

        Minecraft mc = Minecraft.getInstance();
        float scale = (float) (double) mc.options.screenEffectScale().get();
        VoidFeedback.camera(event, domain, ClientEffectClock.ticks(),
            (float) event.getPartialTick(), scale);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        ClientEffectClock.changeLevel(mc.level);
        ClientEffectClock.tick(mc.level, mc.isPaused());

        if (mc.level == null || mc.player == null) return;

        AABB search = mc.player.getBoundingBox().inflate(512.0D);
        List<VoidDomainEntity> found = mc.level.getEntitiesOfClass(VoidDomainEntity.class, search);

        if (found.isEmpty()) {
            UnlimitedVoid existing = VoidDomain.active();
            if (existing != null && existing.expired(ClientEffectClock.ticks())) VoidDomain.clear();
            return;
        }

        VoidDomainEntity entity = found.get(0);
        int remaining = entity.remainingTicks();

        if (VoidDomain.active() == null) {
            VoidDomain.open(entity.position(), entity.look(), entity.domainRadius(), remaining);
        } else if (remaining <= 0) {
            VoidDomain.close();
        } else {
            VoidDomain.reschedule(remaining);
        }

        UnlimitedVoid domain = VoidDomain.active();
        if (domain != null) {
            domain.tick(mc);
            if (domain.expired(ClientEffectClock.ticks())) VoidDomain.clear();
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        release();
    }

    public static void release() {
        VoidDomain.clear();
        WaterSceneCapture.release();
        com.truenins.client.domain.judgment.JudgmentStageCapture.release();
        DomainWorldCapture.release();
    }
}
