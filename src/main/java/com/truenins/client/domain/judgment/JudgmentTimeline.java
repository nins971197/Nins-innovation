package com.truenins.client.domain.judgment;

import com.truenins.TrueNinsMod;
import com.truenins.entity.domain.JudgmentDomainEntity;
import com.truenins.register.TNSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static com.truenins.entity.domain.JudgmentDomainEntity.smooth;

/** Every client schedules impact and light cues from the synchronized domain clock. */
@Mod.EventBusSubscriber(modid=TrueNinsMod.MODID, value=Dist.CLIENT)
public final class JudgmentTimeline {
    public static final int ENTRANCE_HIT=16;
    private static final Map<UUID,Float> PREVIOUS=new HashMap<>();
    private JudgmentTimeline() {}
    public static boolean cold(float age) { return age<132 && (((int)Math.max(0,age-24)/14)&1)==1; }
    public static float gavelAngle(float age,float hit) {
        float t=age-hit;
        if(t < -14 || t > 12)return 0;
        // The head is on the negative X end: negative Z rotation lifts it above the desk.
        if(t<0)return -62*(1-smooth((t+14)/14));
        return -12*(float)Math.sin(Math.PI*smooth(t/12))*(1-t/12);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null){PREVIOUS.clear();return;}
        java.util.Set<UUID> alive=new java.util.HashSet<>();
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof JudgmentDomainEntity d){
            alive.add(d.getUUID()); float age=d.visualAge(0);
            Float previous=PREVIOUS.put(d.getUUID(),age);
            float from=previous==null?Math.max(0,age-1):previous;
            for(int cue:new int[]{ENTRANCE_HIT,132,170})if(from<cue&&age>=cue&&age-cue<4){
                mc.level.playLocalSound(d.getX(),d.getY(),d.getZ(),TNSounds.JUDGMENT_GAVEL.get(),
                    SoundSource.PLAYERS,cue==ENTRANCE_HIT?2f:1.4f,cue==132?.84f:cue==170?.8f:1f,false);
            }
        }
        PREVIOUS.keySet().retainAll(alive);
    }
}
