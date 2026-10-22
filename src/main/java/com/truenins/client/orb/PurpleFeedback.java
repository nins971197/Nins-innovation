package com.truenins.client.orb;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsMod;
import com.truenins.entity.orb.PurpleOrbEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.List;

/** Render-camera feedback only: never changes player aim or sends look packets. */
@Mod.EventBusSubscriber(modid=TrueNinsMod.MODID, value=Dist.CLIENT)
public final class PurpleFeedback {
    private static final List<PurpleOrbEntity> NEARBY = new ArrayList<>();
    private PurpleFeedback() {}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        NEARBY.clear();
        if (mc.level == null || mc.player == null) return;
        double range = Math.max(40, TrueNinsConfig.purpleBlastRadius() * 3);
        NEARBY.addAll(mc.level.getEntitiesOfClass(PurpleOrbEntity.class,
            mc.player.getBoundingBox().inflate(range), PurpleOrbEntity::isAlive));
    }
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        float setting = (float)(double)mc.options.screenEffectScale().get();
        if (setting <= 0 || mc.level == null || mc.isPaused()) return;
        float partial = (float)event.getPartialTick(), pitch=0, yaw=0, roll=0;
        float radius = (float)TrueNinsConfig.purpleBlastRadius();
        for (PurpleOrbEntity orb : NEARBY) {
            if (!orb.isAlive() || orb.level() != mc.level) continue;
            float distance = (float)event.getCamera().getPosition().distanceTo(orb.position());
            float range = 1 - smooth((distance - radius * .45f) / (radius * 2.2f));
            if (range <= 0) continue;
            float time = (orb.tickCount + partial) / 20f, amplitude;
            if (orb.explodeProgress() < 0) {
                float fusion = (orb.phase() + partial) / Math.max(1f, TrueNinsConfig.purpleFusionTicks());
                amplitude = (.055f + .27f * smooth(fusion)) * smooth(fusion * 4);
            } else {
                float elapsed = (orb.explodeProgress() + partial) / 20f;
                // A sharp initial kick followed by lower-frequency pressure and settling.
                amplitude = 3.8f * (float)Math.exp(-elapsed * 4.6)
                    + .65f * (float)Math.exp(-elapsed * 1.8);
            }
            float phase = orb.getId() * .73f, strength = amplitude * range;
            pitch += wave(time, 79, phase) * strength;
            yaw += wave(time, 67, phase + 2) * strength * .66f;
            roll += wave(time, 53, phase + 4) * strength * .48f;
        }
        event.setPitch(event.getPitch() + Mth.clamp(pitch,-4.2f,4.2f) * setting);
        event.setYaw(event.getYaw() + Mth.clamp(yaw,-2.8f,2.8f) * setting);
        event.setRoll(event.getRoll() + Mth.clamp(roll,-1.9f,1.9f) * setting);
    }
    private static float wave(float t,float frequency,float phase) {
        return Mth.sin(t*frequency+phase)*.72f + Mth.sin(t*frequency*1.71f+phase*2)*.28f;
    }
    private static float smooth(float x) { x=Mth.clamp(x,0,1);return x*x*(3-2*x); }
}
