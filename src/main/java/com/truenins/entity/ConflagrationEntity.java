package com.truenins.entity;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsMod;
import com.truenins.register.TNEntities;
import com.truenins.spell.OrbDamage;
import com.truenins.spell.conflagration.ConflagrationPlan;
import com.truenins.spell.conflagration.ConflagrationNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** One controller per dimension; fire rain is choreography, not dozens of ticking projectiles. */
@Mod.EventBusSubscriber(modid=TrueNinsMod.MODID)
public final class ConflagrationEntity extends Entity {
    private ConflagrationPlan plan;
    private UUID owner;
    private int next;
    private ConflagrationPlan.Strike exploding;
    private final Set<UUID> hit = new HashSet<>();
    public ConflagrationEntity(EntityType<?> type,Level level) {
        super(type,level);noPhysics=true;setNoGravity(true);
    }
    public ConflagrationEntity(ServerLevel level,LivingEntity caster) {
        this(TNEntities.CONFLAGRATION.get(),level);owner=caster.getUUID();setPos(caster.position());
        var strikes=new ArrayList<ConflagrationPlan.Strike>();
        double radius=TrueNinsConfig.conflagrationRadius();
        for(int i=0;i<36;i++) {
            double angle=i*2.3999632297+random.nextDouble()*.3;
            double r=radius*Math.sqrt((i+.5)/36.0);
            double x=getX()+Math.cos(angle)*r,z=getZ()+Math.sin(angle)*r;
            var pos=BlockPos.containing(x,getY(),z);
            if(!level.hasChunkAt(pos))continue;
            double y=level.getHeight(Heightmap.Types.MOTION_BLOCKING,pos.getX(),pos.getZ());
            if(Math.abs(y-getY())>60)continue;
            Vec3 to=new Vec3(x,y+.12,z);
            Vec3 from=to.add(-18-random.nextDouble()*18,70+random.nextDouble()*30,12+random.nextDouble()*12);
            if(!loadedPath(level,from,to))continue;
            var roof=level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
            if(roof.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)to=roof.getLocation().add(0,.08,0);
            strikes.add(new ConflagrationPlan.Strike(from,to,60+i*3,38+random.nextInt(15),
                i%5==0?2.6f:1.0f+random.nextFloat()*.6f,random.nextInt(10000),false));
        }
        // The giant visibly meets the roof / ground at the same tick as the server explosion.
        Vec3 impact=position().add(0,.12,0);
        int floor=level.getHeight(Heightmap.Types.MOTION_BLOCKING,blockPosition().getX(),blockPosition().getZ());
        if(Math.abs(floor-getY())<60)impact=new Vec3(getX(),floor+.12,getZ());
        // The final body is deliberately much larger than the rain stones.  Its centre starts
        // above the cloud deck; the renderer reveals only the lower shell until it breaks through.
        // Keeping the impact point unchanged preserves the server blast and terrain rules.
        strikes.add(new ConflagrationPlan.Strike(impact.add(-22,82,12),impact,155,85,60,random.nextInt(10000),true));
        strikes.sort(Comparator.comparingInt(ConflagrationPlan.Strike::impact));
        plan=new ConflagrationPlan(getUUID(),level.getGameTime(),position(),TrueNinsConfig.conflagrationGiantRadius(),strikes);
    }
    public static boolean available(ServerLevel level) {
        for(var e:level.getAllEntities())if(e instanceof ConflagrationEntity&&e.isAlive())return false;
        return true;
    }
    public ConflagrationPlan plan(){return plan;}
    private static boolean loadedPath(ServerLevel level,Vec3 from,Vec3 to) {
        for(int i=0;i<=12;i++)if(!level.hasChunkAt(BlockPos.containing(from.lerp(to,i/12.))))return false;
        return true;
    }
    @Override public void tick() {
        super.tick();if(!(level() instanceof ServerLevel server))return;
        if(plan==null){discard();return;}
        int age=(int)(server.getGameTime()-plan.start());
        if(age==1)ConflagrationNetwork.broadcast(server,plan,false);
        if(age==35||age==175)server.playSound(null,getX(),getY()+12,getZ(),SoundEvents.ENDER_DRAGON_GROWL,SoundSource.PLAYERS,4,.55f);
        int pulseAge=age-ConflagrationPlan.BREAKTHROUGH_START;
        if(pulseAge>=0&&pulseAge%ConflagrationPlan.BREAKTHROUGH_INTERVAL==0
            &&pulseAge/ConflagrationPlan.BREAKTHROUGH_INTERVAL<ConflagrationPlan.BREAKTHROUGH_PULSES) {
            int pulse=pulseAge/ConflagrationPlan.BREAKTHROUGH_INTERVAL;
            server.playSound(null,getX(),getY()+35-pulse*5,getZ(),
                com.truenins.register.TNSounds.CONFLAGRATION_BREAKTHROUGH.get(),SoundSource.PLAYERS,2.4f-pulse*.3f,1f);
        }
        while(next<plan.strikes().size()&&plan.strikes().get(next).impact()<=age) {
            var strike=plan.strikes().get(next++);
            if(!server.hasChunkAt(BlockPos.containing(strike.to())))continue;
            // Roof intersection was fixed in the shared plan, so the visible contact and hit agree.
            Vec3 at=strike.to();
            exploding=new ConflagrationPlan.Strike(strike.from(),at,strike.launch(),strike.flight(),strike.size(),strike.seed(),strike.giant());
            hit.clear();
            float range=strike.giant()?plan.blastRadius():5f;
            boolean terrain=TrueNinsConfig.conflagrationBreakBlocks()&&server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            float power=range*.5f;
            if(!strike.giant()) {
                // A stone that lands near the rain centre digs noticeably harder than one that
                // lands on the rim, which keeps the spell from flattening the whole disc evenly.
                double span=Math.max(1.0D,TrueNinsConfig.conflagrationRadius());
                double near=Mth.clamp(1.0D-strike.to().distanceTo(plan.center())/span,0.0D,1.0D);
                power*=(float)(1.0D+(TrueNinsConfig.conflagrationMeteorBlastScale()-1.0D)*near*near);
            }
            // Vanilla queries entities out to twice its explosion power. Our configured
            // damage radius is therefore power * 2, then clipped to the sphere in detonate().
            server.explode(this,null,null,at.x,at.y,at.z,power,false,
                terrain?Level.ExplosionInteraction.BLOCK:Level.ExplosionInteraction.NONE,false);
            if(strike.giant()&&terrain) {
                // One extra blast centred below the contact point.  The regular explosion only
                // ever scours a bowl at the surface; this one digs the pit the finale promises.
                // It goes through the vanilla explosion path, so mobGriefing and every Forge
                // explosion/protection hook still decide what survives.
                double crater=TrueNinsConfig.conflagrationGiantCraterRadius();
                if(crater>0.0D) {
                    double depth=TrueNinsConfig.conflagrationGiantCraterDepth();
                    server.explode(this,null,null,at.x,at.y-depth,at.z,(float)(crater*.5D),false,
                        Level.ExplosionInteraction.BLOCK,false);
                }
            }
            server.playSound(null,at.x,at.y,at.z,SoundEvents.GENERIC_EXPLODE,SoundSource.PLAYERS,
                strike.giant()?5f:1.8f,strike.giant()?.55f:.75f+random.nextFloat()*.3f);
            exploding=null;
        }
        if(age>=ConflagrationPlan.DURATION)discard();
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void detonate(ExplosionEvent.Detonate event) {
        if(!(event.getExplosion().getDirectSourceEntity() instanceof ConflagrationEntity c)||c.exploding==null)return;
        var s=c.exploding;
        LivingEntity caster=((ServerLevel)c.level()).getEntity(c.owner) instanceof LivingEntity l?l:null;
        double range=s.giant()?c.plan.blastRadius():5;
        float damage=s.giant()?TrueNinsConfig.conflagrationGiantDamage():TrueNinsConfig.conflagrationDamage();
        for(Entity e:event.getAffectedEntities())if(e instanceof LivingEntity target&&!e.getUUID().equals(c.owner)&&c.hit.add(e.getUUID())) {
            double distance=target.getBoundingBox().getCenter().distanceTo(s.to());
            if(distance>range||target.isSpectator())continue;
            double exposure=Explosion.getSeenPercent(s.to(),target);
            float factor=(float)((1-distance/range)*exposure);
            if(factor<=0)continue;
            OrbDamage.apply(target,c,caster,damage*(.3f+.7f*factor),"truenins:eternal_conflagration");
            target.setSecondsOnFire(s.giant()?8:4);
            target.push((target.getX()-s.to().x)/Math.max(1,distance)*factor,
                .3*factor,(target.getZ()-s.to().z)/Math.max(1,distance)*factor);
            target.hurtMarked=true;
        }
        event.getAffectedEntities().clear();
    }
    @Override public void remove(RemovalReason reason) {
        if(!isRemoved()&&level() instanceof ServerLevel s&&plan!=null)ConflagrationNetwork.broadcast(s,plan,true);
        super.remove(reason);
    }
    @Override protected void defineSynchedData(){}
    @Override protected void readAdditionalSaveData(CompoundTag tag){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
    @Override public boolean isAttackable(){return false;}
}
