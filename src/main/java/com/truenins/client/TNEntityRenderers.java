package com.truenins.client;

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
        event.registerEntityRenderer(TNEntities.BLUE_ORB.get(), TNOrbRenderer::new);
        event.registerEntityRenderer(TNEntities.RED_ORB.get(), TNOrbRenderer::new);
    }
}
