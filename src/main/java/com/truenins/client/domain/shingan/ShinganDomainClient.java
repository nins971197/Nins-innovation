package com.truenins.client.domain.shingan;

import com.truenins.TrueNinsMod;
import com.truenins.entity.domain.ShinganDomainEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client side of 真赝相爱: while the player is inside a domain, the atmosphere
 * gains the reference's purple and pink haze. State resets on world changes.
 */
@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT)
public final class ShinganDomainClient {

    private static ShinganDomainEntity active;
    private static float strength;

    private ShinganDomainClient() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            active = null;
            strength = 0;
            return;
        }
        ShinganDomainEntity best = null;
        double bestDist = 512.0D * 512.0D;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ShinganDomainEntity domain && domain.isAlive()) {
                double d = domain.distanceToSqr(mc.player);
                double r2 = domain.domainRadius() * domain.domainRadius();
                if (d <= r2 && d < bestDist) {
                    bestDist = d;
                    best = domain;
                }
            }
        }
        active = best;
        float desired = best == null ? 0 : Math.min(1, (float)(Math.sqrt(bestDist) < best.domainRadius() - 2 ? 1
            : (best.domainRadius() - Math.sqrt(bestDist)) / 2)) * best.appearance(0);
        strength += (desired - strength) * .18f;
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (strength < .005f || event.getCamera().getFluidInCamera() != FogType.NONE) return;
        event.setRed(event.getRed() + (.30f - event.getRed()) * strength);
        event.setGreen(event.getGreen() + (.095f - event.getGreen()) * strength);
        event.setBlue(event.getBlue() + (.38f - event.getBlue()) * strength);
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (active == null || strength < .005f || event.getCamera().getFluidInCamera() != FogType.NONE
                || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        float near = Math.min(event.getNearPlaneDistance(), active.domainRadius() * .42f);
        float far = Math.min(event.getFarPlaneDistance(), active.domainRadius() * 1.25f);
        event.setNearPlaneDistance(event.getNearPlaneDistance() + (near - event.getNearPlaneDistance()) * strength);
        event.setFarPlaneDistance(event.getFarPlaneDistance() + (far - event.getFarPlaneDistance()) * strength);
        event.setCanceled(true);
    }
}
