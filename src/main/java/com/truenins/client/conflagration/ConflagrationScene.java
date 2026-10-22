package com.truenins.client.conflagration;

import com.truenins.TrueNinsMod;
import com.truenins.client.vfx.WorldEffect;
import com.truenins.client.vfx.WorldVfxRenderer;
import com.truenins.spell.conflagration.ConflagrationNetwork;
import com.truenins.spell.conflagration.ConflagrationPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Two bounded scene passes: no terrain replacement, weather writes or per-fireball particles. */
@Mod.EventBusSubscriber(modid=TrueNinsMod.MODID,value=Dist.CLIENT)
public final class ConflagrationScene {
    private static ShaderInstance sky,meteors;
    private static ConflagrationNetwork.Scene scene;
    private static final int SLOTS=20;
    private static final List<ConflagrationPlan.Strike> visible=new ArrayList<>();
    public static void setSky(ShaderInstance shader){sky=shader;}
    public static void setMeteors(ShaderInstance shader){meteors=shader;}
    public static void receive(ConflagrationNetwork.Scene packet) {
        if(packet.stop()) {if(scene!=null&&scene.plan().id().equals(packet.plan().id()))scene=null;}
        else scene=packet;
    }
    public static ConflagrationPlan active(){return valid()?scene.plan():null;}
    private static boolean valid() {
        var mc=Minecraft.getInstance();
        return scene!=null&&mc.level!=null&&scene.dimension().equals(mc.level.dimension().location())
            &&mc.level.getGameTime()-scene.plan().start()<ConflagrationPlan.DURATION;
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){scene=null;visible.clear();}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        // A dimension packet can arrive before the client has switched levels. Keep it pending;
        // valid() gates rendering until the matching world exists. Logout always clears it.
        if(scene!=null&&Minecraft.getInstance().level!=null&&
            scene.dimension().equals(Minecraft.getInstance().level.dimension().location())&&
            Minecraft.getInstance().level.getGameTime()-scene.plan().start()>=ConflagrationPlan.DURATION)scene=null;
    }
    public static void collect(List<WorldEffect> effects,float partial) {
        if(!valid())return;
        var mc=Minecraft.getInstance();var plan=scene.plan();
        float ticks=mc.level.getGameTime()-plan.start()+partial;
        float age=ticks/20f;
        float layer=layerFade(age);
        effects.add(new WorldEffect(sky,plan.center(),-100,true,false,(shader,frame)->{
            shader.safeGetUniform("Age").set(age);
            shader.safeGetUniform("Visibility").set(layer);
            shader.safeGetUniform("FeedbackScale").set((float)(double)mc.options.screenEffectScale().get());
        }));
        if(meteors==null)return;
        Vec3 camera=mc.gameRenderer.getMainCamera().getPosition();
        visible.clear();
        for(var s:plan.strikes())if(ticks>=s.launch()&&ticks<s.impact()+(s.giant()?56:24)
            &&s.to().distanceToSqr(camera)<384*384)visible.add(s);
        // Always keep the giant; remaining slots favour projected size / proximity.
        visible.sort(Comparator.comparingDouble(s->s.giant()?-1e9:s.head(ticks).distanceToSqr(camera)/(s.size()*s.size())));
        if(visible.size()>SLOTS)visible.subList(SLOTS,visible.size()).clear();
        visible.sort(Comparator.comparingDouble((ConflagrationPlan.Strike s)->s.head(ticks).distanceToSqr(camera)).reversed());
        effects.add(new WorldEffect(meteors,plan.center(),8,true,false,(shader,frame)->{
            shader.safeGetUniform("Age").set(age);
            shader.safeGetUniform("BlastRadius").set(plan.blastRadius());
            shader.safeGetUniform("Breakthrough").set(ConflagrationPlan.BREAKTHROUGH_START/20f,
                ConflagrationPlan.BREAKTHROUGH_INTERVAL/20f,(float)ConflagrationPlan.BREAKTHROUGH_PULSES);
            shader.safeGetUniform("FeedbackScale").set((float)(double)mc.options.screenEffectScale().get());
            // Ends the pass with the scene instead of vanishing on the last tick.
            shader.safeGetUniform("Visibility").set(layer);
            int count=Math.min(SLOTS,visible.size());shader.safeGetUniform("Count").set(count);
            for(int i=0;i<SLOTS;i++) {
                if(i>=count){shader.safeGetUniform("Head"+i).set(0f,0f,0f,0f);continue;}
                var s=visible.get(i);float impactAge=(ticks-s.impact())/20f;
                Vec3 head=s.head(ticks).subtract(frame.camera());
                // Giant core sits above its contact point, so its underside lands at impact.
                if(s.giant())head=head.add(0,s.size(),0);
                float grow=s.giant()?1f:.18f+.82f*smooth((ticks-s.launch())/8f);
                shader.safeGetUniform("Head"+i).set((float)head.x,(float)head.y,(float)head.z,s.size()*grow);
                Vec3 tail=s.from().subtract(s.to()).normalize().scale(s.giant()?30:28+s.size()*12);
                shader.safeGetUniform("Tail"+i).set((float)tail.x,(float)tail.y,(float)tail.z,
                    impactAge>=0?-.001f-impactAge:1f+(ticks-s.launch())/s.flight());
                shader.safeGetUniform("Seed"+i).set((float)s.seed());
            }
        }));
    }

    /** Scene-wide fade: the pass must arrive with the sky and leave with it. */
    private static float layerFade(float age) {
        float in=smooth(age/.45f);
        float out=smooth((ConflagrationPlan.DURATION/20f-age)/1.1f);
        return in*out;
    }

    public static boolean rendering(){return valid();}
    @SubscribeEvent public static void fog(ViewportEvent.ComputeFogColor event) {
        if(!valid())return;
        var mc=Minecraft.getInstance();
        float age=(mc.level.getGameTime()-scene.plan().start()+(float)event.getPartialTick())/20f;
        float amount=smooth(age/3.2f)*(1-smooth((age-12.4f)/2.6f))*.65f;
        event.setRed(Mth.lerp(amount,event.getRed(),.105f));
        event.setGreen(Mth.lerp(amount,event.getGreen(),.095f));
        event.setBlue(Mth.lerp(amount,event.getBlue(),.065f));
    }
    private static float smooth(float x){x=Mth.clamp(x,0,1);return x*x*(3-2*x);}
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if(!valid())return;
        var mc=Minecraft.getInstance();if(mc.isPaused())return;
        float age=mc.level.getGameTime()-scene.plan().start()+(float)event.getPartialTick(),strength=0;
        Vec3 eye=event.getCamera().getPosition();
        for(var s:scene.plan().strikes()) {
            float dt=(age-s.impact())/20f;
            float attenuation=Mth.clamp(1-(float)eye.distanceTo(s.to())/(s.giant()?160:35),0,1);
            if(dt>=0&&dt<3)strength+=(s.giant()?4.6f:.35f)*Math.exp(-dt*(s.giant()?2.1:6))*attenuation;
            if(s.giant()&&age>s.launch()&&dt<0) {
                strength+=.30f*Mth.clamp((age-s.launch())/s.flight(),0,1)*attenuation;
                for(int pulse=0;pulse<ConflagrationPlan.BREAKTHROUGH_PULSES;pulse++) {
                    float elapsed=(age-ConflagrationPlan.BREAKTHROUGH_START
                        -pulse*ConflagrationPlan.BREAKTHROUGH_INTERVAL)/20f;
                    if(elapsed>=0&&elapsed<1.4f)
                        strength+=(.85f-pulse*.1f)*smooth(elapsed/.05f)*Math.exp(-elapsed*5f)*attenuation;
                }
            }
        }
        strength=Math.min(4.8f,strength)*(float)(double)mc.options.screenEffectScale().get();
        float time=age/20f;
        event.setPitch(event.getPitch()+Mth.sin(time*73)*strength);
        event.setYaw(event.getYaw()+Mth.sin(time*61+1)*strength*.6f);
        event.setRoll(event.getRoll()+Mth.sin(time*49+3)*strength*.42f);
    }
}
