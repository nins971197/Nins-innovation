package com.truenins.validation;

import com.truenins.TrueNinsMod;
import com.truenins.TrueNinsConfig;
import com.truenins.compat.irons.TNIronsCompat;
import com.truenins.entity.ConflagrationEntity;
import com.truenins.client.conflagration.ConflagrationScene;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Natural Iron cast, packet playback and staged real-client captures; excluded from release. */
@Mod.EventBusSubscriber(modid=TrueNinsMod.MODID,value=Dist.CLIENT)
public final class ConflagrationChecks {
    private static volatile int tick;
    private static boolean ready,oldTerrain,finished;
    private static int shot,impacts,giantImpacts;
    private static ConflagrationEntity spell;
    private static Cow inside,outside,owner,rimInside,rimOutside;
    private static float preHit;
    private static int breakthroughSounds;
    private static net.minecraft.world.entity.decoration.ArmorStand camera;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static boolean enabled(){return Boolean.getBoolean("truenins.conflagrationCheck");}
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundEvent event){
        var sound=event.getSound();
        if(enabled()&&tick<300&&sound!=null
            &&sound.getLocation().toString().equals("truenins:conflagration_breakthrough")) {
            breakthroughSounds++;
            TrueNinsMod.LOGGER.info("CONFLAGRATION_CHECK: pressure boom {} reached client sound engine at tick={}",
                breakthroughSounds,tick);
        }
    }
    private static Cow dummy(net.minecraft.server.level.ServerLevel level,Vec3 pos){
        var c=EntityType.COW.create(level);c.setNoAi(true);c.setNoGravity(true);c.setPos(pos);
        c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);c.setHealth(500);level.addFreshEntity(c);return c;
    }
    @SubscribeEvent public static void join(PlayerEvent.PlayerLoggedInEvent event){
        if(!enabled()||ready||!(event.getEntity() instanceof ServerPlayer p))return;
        ready=true;var level=p.serverLevel();
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,p.server);
        for(var e:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(e!=p)e.discard();
        for(int x=-52;x<=52;x++)for(int z=-52;z<=52;z++){
            level.setBlock(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState(),2);
            for(int y=120;y<=146;y++)if(!level.isEmptyBlock(new BlockPos(x,y,z)))level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
        }
        // A retained wall provides depth-occlusion evidence during the rain.
        for(int x=16;x<=22;x++)for(int y=120;y<=130;y++)level.setBlock(new BlockPos(x,y,-8),Blocks.BEDROCK.defaultBlockState(),2);
        p.setHealth(p.getMaxHealth());p.deathTime=0;p.clearFire();p.getFoodData().setFoodLevel(20);
        p.setGameMode(GameType.SURVIVAL);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();
        p.teleportTo(level,0,120,0,0,-24);level.setDayTime(6000);
        oldTerrain=TrueNinsConfig.conflagrationBreakBlocks();TrueNinsConfig.CONFLAGRATION_BREAK_BLOCKS.set(false);
        TrueNinsConfig.CONFLAGRATION_GIANT_RADIUS.set(25.0);
    }
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate event){
        if(!enabled()||!(event.getExplosion().getDirectSourceEntity() instanceof ConflagrationEntity c)||c!=spell)return;
        impacts++;if(event.getExplosion().getPosition().distanceTo(c.position())<1)giantImpacts++;
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){
        if(!enabled()||!ready||event.phase!=TickEvent.Phase.END)return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        var p=server.getPlayerList().getPlayers().get(0);var level=p.serverLevel();tick++;
        if(tick==25){
            // Previous interrupted visual runs may have saved a dead player. Use vanilla respawn
            // to reset its internal death/removed state, rather than just assigning positive HP.
            p=server.getPlayerList().respawn(p,false);
            p.setGameMode(GameType.SURVIVAL);p.teleportTo(level,0,120,0,0,-24);
            p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();
        }
        if(tick==40){
            var s=TNIronsCompat.ETERNAL_CONFLAGRATION.get();var data=MagicData.getPlayerMagicData(p);
            check(SpellRegistry.getSpell("truenins:eternal_conflagration")==s,"registry");
            check(s.getSchoolType()==TNIronsCompat.CURSE.get()&&s.getManaCost(1)==500,"curse / mana");
            p.getAttribute(AttributeRegistry.MAX_MANA.get()).setBaseValue(1000);
            p.getAttribute(AttributeRegistry.MANA_REGEN.get()).setBaseValue(0);
            data.resetCastingState();data.getPlayerCooldowns().removeCooldown(s.getSpellId());data.setMana(1000);
            var eligibility=s.canBeCastedBy(1,CastSource.SPELLBOOK,data,p);
            check(eligibility.isSuccess(),"cast eligibility: "+eligibility.message+" mana="+data.getMana());
            check(s.attemptInitiateCast(ItemStack.EMPTY,1,level,p,CastSource.SPELLBOOK,true,"mainhand"),"natural cast rejected; alive="+p.isAlive()+" precheck="+s.checkPreCastConditions(level,1,p,data));
        }
        if(tick==45){
            for(var e:level.getAllEntities())if(e instanceof ConflagrationEntity c)spell=c;
            check(spell!=null,"natural cast controller missing");
            check(Math.abs(MagicData.getPlayerMagicData(p).getMana()-500)<.01,"mana not 500: "+MagicData.getPlayerMagicData(p).getMana());
            check(!TNIronsCompat.ETERNAL_CONFLAGRATION.get().checkPreCastConditions(level,1,p,MagicData.getPlayerMagicData(p)),"overlap allowed");
            check(spell.plan().strikes().size()==37,"loaded terrain should yield 36 fireballs + giant");
            TrueNinsMod.LOGGER.info("CONFLAGRATION_CHECK: registered curse, natural cast, mana 500/1000, overlap rejection and 37-strike plan passed");
            Vec3 impact=spell.plan().strikes().stream().filter(s->s.giant()).findFirst().orElseThrow().to();
            inside=dummy(level,impact.add(9,0,0));outside=dummy(level,impact.add(80,0,0));
            preHit=inside.getHealth();
        }
        if(tick==282){
            check(giantImpacts==1,"giant not detonated exactly once: "+giantImpacts);
            check(inside.getHealth()<preHit,"giant damage missing");
            check(outside.getHealth()==500,"outside radius damaged");
            check(!p.isOnFire(),"caster ignited");
            check(level.getBlockState(new BlockPos(0,119,0)).is(Blocks.STONE),"breakBlocks=false changed floor");
            check(spell.plan().blastRadius()==25,"server blast radius not 25");
            check(rimInside.getHealth()<500,"24.5 block target should be inside final blast");
            check(rimOutside.getHealth()==500,"25.5 block target should be outside final blast");
            TrueNinsMod.LOGGER.info("CONFLAGRATION_CHECK: radius 25 boundary passed: 24.5 blocks HP={}, 25.5 blocks HP=500",rimInside.getHealth());
            TrueNinsMod.LOGGER.info("CONFLAGRATION_CHECK: {} impacts, giant exactly once, inside HP={}, outside HP=500, caster not ignited, breakBlocks=false preserved floor",impacts,inside.getHealth());
        }
        if(tick==265){
            // Rain has ended. Isolate the giant blast's new 25-block boundary.
            Vec3 impact=spell.plan().strikes().stream().filter(s->s.giant()).findFirst().orElseThrow().to();
            // Use the server's actual contact, not the world's origin: respawn / client movement
            // can shift the natural cast position slightly before the Iron cast completes.
            rimInside=dummy(level,impact.add(-24.5,0,0));rimOutside=dummy(level,impact.add(-25.5,0,0));
            TrueNinsMod.LOGGER.info("CONFLAGRATION_CHECK: actual giant contact={}, boundary targets={} / {}",impact,rimInside.position(),rimOutside.position());
        }
        if(tick==350){
            check(!spell.isAlive()&&ConflagrationEntity.available(level),"controller not cleaned");
            // Second cast tests genuine terrain destruction and living caster exemption.
            owner=dummy(level,new Vec3(0,120,0));
            p.setGameMode(GameType.SPECTATOR); // Observe the NPC cast without dying in its blast.
            TrueNinsConfig.CONFLAGRATION_BREAK_BLOCKS.set(true);
            spell=new ConflagrationEntity(level,owner);level.addFreshEntity(spell);
            impacts=0;giantImpacts=0;
        }
        if(tick==595){
            check(owner.getHealth()==500&&!owner.isOnFire(),"living caster immunity failed");
            check(!level.getBlockState(new BlockPos(0,119,0)).is(Blocks.STONE),"breakBlocks=true did not destroy floor");
            TrueNinsMod.LOGGER.info("CONFLAGRATION_CHECK: breakBlocks=true destroyed floor; living caster HP 500 and no fire; Forge detonate events received");
            TrueNinsConfig.CONFLAGRATION_BREAK_BLOCKS.set(oldTerrain);
        }
        if(tick==690){
            check(ConflagrationEntity.available(level),"second controller leaked");
            TrueNinsConfig.CONFLAGRATION_BREAK_BLOCKS.set(false);
            spell=new ConflagrationEntity(level,owner);level.addFreshEntity(spell);
        }
        if(tick==740){
            var domain=new com.truenins.entity.domain.ShinganDomainEntity(level,owner,100,24,1);
            level.addFreshEntity(domain);
        }
        if(tick==1000)TrueNinsConfig.CONFLAGRATION_BREAK_BLOCKS.set(oldTerrain);
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event){
        if(!enabled()||!ready||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.renderDistance().set(8);mc.options.fov().set(75);
        if(camera==null)camera=new net.minecraft.world.entity.decoration.ArmorStand(mc.level,0,124,-32);
        double x=0,y=123,z=-32;float pitch=-24,yaw=0;
        if(tick>=70&&tick<125){pitch=-76;z=-4;}
        if(tick>=125&&tick<190){pitch=-35;x=8;z=-35;}
        if(tick>=220&&tick<270){pitch=-24;z=-35;}
        if(tick>=310&&tick<335){x=900;pitch=-70;z=0;}
        if(tick>=350){x=35;z=-45;pitch=-8;y=127;yaw=38;}
        if(tick>=750&&tick<780){x=0;z=-8;pitch=-28;y=123;yaw=0;}
        camera.setPos(x,y,z);camera.xo=x;camera.yo=y;camera.zo=z;
        camera.setYRot(yaw);camera.yRotO=yaw;camera.setXRot(pitch);camera.xRotO=pitch;mc.setCameraEntity(camera);
        int[] shots={42,60,85,110,140,165,185,225,231,240,245,249,255,265,275,280,282,285,290,300,326,352,450,560,575,585,590,592,600,610,650,770,810,930,980};
        for(int s:shots)if(tick>=s&&shot<s){
            shot=s;
            if(s==85)check(ConflagrationScene.active()!=null,"S2C scene missing");
            if(s==352)check(ConflagrationScene.active()!=null,"second cast packet missing");
            if(s==300){
                check(breakthroughSounds==3,"expected exactly three pressure booms: "+breakthroughSounds);
                TrueNinsMod.LOGGER.info("CONFLAGRATION_CHECK: three pressure booms reached client sound engine");
            }
            Screenshot.grab(mc.gameDirectory,"256-conflagration-"+s+".png",mc.getMainRenderTarget(),c->TrueNinsMod.LOGGER.info("CONFLAGRATION screenshot: {}",c.getString()));break;
        }
        if(tick>=1020&&!finished){
            finished=true;
            check(ConflagrationScene.active()==null,"client scene leaked after end");
            TrueNinsMod.LOGGER.info("CONFLAGRATION_CHECK: client scene received and expired; staged captures complete");mc.stop();
        }
    }
}
