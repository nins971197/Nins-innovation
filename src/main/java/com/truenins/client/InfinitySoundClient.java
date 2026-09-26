package com.truenins.client;

import com.truenins.TrueNinsMod;
import com.truenins.register.TNMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT)
public final class InfinitySoundClient {

    private static final ResourceLocation SOUND =
        new ResourceLocation(TrueNinsMod.MODID, "rain_love");

    private static TNLoopingSound active;

    private InfinitySoundClient() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        boolean want = mc.player != null && mc.level != null
            && mc.player.hasEffect(TNMobEffects.INFINITY_FIELD.get());

        if (want) {
            if (active == null || !mc.getSoundManager().isActive(active)) {
                active = new TNLoopingSound(SOUND, 0.7F);
                mc.getSoundManager().play(active);
            }
        } else if (active != null) {
            mc.getSoundManager().stop(active);
            active = null;
        }
    }
}
