package com.truenins.client;

import com.truenins.client.domain.shingan.TNShinganDecorRenderer;
import com.truenins.client.domain.shingan.TNShinganDomainRenderer;
import com.truenins.client.domain.voiddomain.TNVoidDomainRenderer;
import com.truenins.client.orb.TNOrbRenderer;
import com.truenins.client.orb.TNShardRenderer;

import com.truenins.TrueNinsMod;
import com.truenins.register.TNEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TNEntityRenderers {

    private TNEntityRenderers() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TNEntities.CONFLAGRATION.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(TNEntities.BLUE_ORB.get(), TNOrbRenderer::new);
        event.registerEntityRenderer(TNEntities.RED_ORB.get(), TNOrbRenderer::new);
        event.registerEntityRenderer(TNEntities.PURPLE_ORB.get(), TNOrbRenderer::new);
        event.registerEntityRenderer(TNEntities.ORB_SHARD.get(), TNShardRenderer::new);
        event.registerEntityRenderer(TNEntities.VOID_DOMAIN.get(), TNVoidDomainRenderer::new);
        event.registerEntityRenderer(TNEntities.SHINGAN_DOMAIN.get(), TNShinganDomainRenderer::new);
        event.registerEntityRenderer(TNEntities.SHINGAN_DECOR.get(), TNShinganDecorRenderer::new);
        event.registerEntityRenderer(TNEntities.JUDGMENT_DOMAIN.get(), com.truenins.client.domain.judgment.JudgmentDomainRenderer::new);
    }
}
