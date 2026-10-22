package com.truenins.validation;

import com.truenins.TrueNinsMod;
import com.truenins.entity.orb.PurpleOrbEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Observe actual Forge camera events; no production-only testing hooks. */
@Mod.EventBusSubscriber(modid=TrueNinsMod.MODID,value=Dist.CLIENT)
public final class PurpleFeedbackChecks {
    private static float beforePitch,beforeYaw,beforeRoll,fusion,blast,disabled;
    private static double original;
    private static boolean muted,done;
    private static int mutedFrames;
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void before(ViewportEvent.ComputeCameraAngles e) {
        beforePitch=e.getPitch();beforeYaw=e.getYaw();beforeRoll=e.getRoll();
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void after(ViewportEvent.ComputeCameraAngles e) {
        if(!Boolean.getBoolean("truenins.visualCheck")||(Boolean.getBoolean("truenins.judgmentCheck") || Boolean.getBoolean("truenins.conflagrationCheck")))return;
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        for(var orb:mc.level.getEntitiesOfClass(PurpleOrbEntity.class,mc.player.getBoundingBox().inflate(30))) {
            float delta=Math.max(Math.abs(e.getPitch()-beforePitch),Math.max(Math.abs(e.getYaw()-beforeYaw),Math.abs(e.getRoll()-beforeRoll)));
            if(muted){disabled=Math.max(disabled,delta);mutedFrames++;}
            else if(orb.explodeProgress()<0)fusion=Math.max(fusion,delta);
            else blast=Math.max(blast,delta);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END||done||!Boolean.getBoolean("truenins.visualCheck")||(Boolean.getBoolean("truenins.judgmentCheck") || Boolean.getBoolean("truenins.conflagrationCheck")))return;
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        for(var orb:mc.level.getEntitiesOfClass(PurpleOrbEntity.class,mc.player.getBoundingBox().inflate(30))) {
            if(!muted&&orb.explodeProgress()>=20){
                original=mc.options.screenEffectScale().get();mc.options.screenEffectScale().set(0D);muted=true;
            } else if(muted&&orb.explodeProgress()>=25) {
                mc.options.screenEffectScale().set(original);muted=false;done=true;
                if(fusion<=0||blast<=fusion||mutedFrames==0||disabled>.00001f)
                    throw new AssertionError("Camera feedback: "+fusion+" / "+blast+" / "+disabled+" frames="+mutedFrames);
                TrueNinsMod.LOGGER.info("PURPLE_CAMERA_CHECK: fusion max={} degrees, blast max={}, disabled max={}, frames={}; screen-effect option restored",fusion,blast,disabled,mutedFrames);
            }
        }
    }
}
