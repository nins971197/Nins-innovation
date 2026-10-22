package com.truenins.validation;

import com.truenins.TrueNinsMod;
import com.truenins.entity.domain.JudgmentDomainEntity;
import com.truenins.spell.domain.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.*;

/** Actual integrated-server checks. Reflection fixes verdicts in the development harness only. */
@Mod.EventBusSubscriber(modid=TrueNinsMod.MODID, value=Dist.CLIENT)
public final class JudgmentChecks {
    private static boolean ready, checked;
    // Wait out vanilla's login protection before checking ordinary physical damage.
    private static volatile int tick=-70;
    private static int shot=-1;
    private static JudgmentDomainEntity court;
    private static Zombie defendant, bystander, outerBystander;
    private static net.minecraft.world.entity.decoration.ArmorStand camera;
    private static volatile boolean entranceSound;
    private static Vec3 initial;
    private static final BlockPos CHEST=new BlockPos(3,119,3);
    private static net.minecraft.nbt.CompoundTag originalChest;
    private static float plainLoss, playerMax;
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundEvent e) {
        var sound=e.getSound();
        if(Boolean.getBoolean("truenins.judgmentCheck")&&sound!=null
                &&sound.getLocation().toString().equals("truenins:judgment_gavel")&&tick>=42&&tick<=65) {
            entranceSound=true;
            // PlaySoundEvent can fire before the client SoundInstance has resolved its
            // backing Sound.  Checking only the stable resource id keeps this harness
            // diagnostic from turning an otherwise valid audio check into an exception.
            TrueNinsMod.LOGGER.info("JUDGMENT_AUDIO_CHECK: entrance gavel reached client sound engine at tick={}, id={}",
                tick, sound.getLocation());
        }
    }
    @SubscribeEvent public static void join(PlayerEvent.PlayerLoggedInEvent e) {
        if(!Boolean.getBoolean("truenins.judgmentCheck")||ready||!(e.getEntity() instanceof ServerPlayer p))return;
        ready=true;
        ServerLevel level=p.serverLevel();
        for(Entity entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))
            if(entity!=p)entity.discard();
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,p.server);
        for(int x=-40;x<=40;x++)for(int z=-40;z<=40;z++){
            level.setBlock(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState(),2);
            for(int y=120;y<135;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
        }
        level.setBlock(CHEST,Blocks.CHEST.defaultBlockState(),3);
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(CHEST);
        chest.setItem(0,new ItemStack(Items.DIAMOND,7));
        originalChest=chest.saveWithFullMetadata();
        p.getInventory().clearContent();p.removeAllEffects();
        p.setGameMode(GameType.SURVIVAL);p.teleportTo(level,0,120,-4,0,0);
        level.setDayTime(6000);
    }
    private static Zombie zombie(ServerLevel level,double x,double z){
        Zombie mob=EntityType.ZOMBIE.create(level);mob.setPos(x,120,z);mob.setPersistenceRequired();
        mob.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));level.addFreshEntity(mob);return mob;
    }
    private static void call(JudgmentDomainEntity d,String name,Class<?>[] types,Object... args){
        try{Method m=JudgmentDomainEntity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(d,args);}
        catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
    }
    private static void death(JudgmentDomainEntity d,LivingEntity target,LivingEntity caster){
        call(d,"execute",new Class<?>[]{LivingEntity.class,LivingEntity.class},target,caster);
    }
    private static void forceDeath(JudgmentDomainEntity d){
        try{
            Field field=JudgmentDomainEntity.class.getDeclaredField("VERDICT");field.setAccessible(true);
            @SuppressWarnings("unchecked") var key=(net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null);
            d.getEntityData().set(key,JudgmentRules.DEATH);
        }catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void rules(ServerLevel level,ServerPlayer p){
        Zombie owner=zombie(level,140,0),enemy=zombie(level,140,5);
        Cow cow=EntityType.COW.create(level);cow.setPos(145,120,5);level.addFreshEntity(cow);
        check(JudgmentRules.kind(cow)==JudgmentRules.Kind.INNOCENT,"passive classification");
        check(JudgmentRules.kind(enemy)==JudgmentRules.Kind.HOSTILE,"hostile classification");
        check(JudgmentRules.kind(p)==JudgmentRules.Kind.PLAYER,"player classification");
        var boss=EntityType.WARDEN.create(level);boss.setPos(150,120,5);level.addFreshEntity(boss);
        check(JudgmentRules.kind(boss)==JudgmentRules.Kind.BOSS,"boss tag classification");
        var acquittal=new JudgmentDomainEntity(level,owner,cow);level.addFreshEntity(acquittal);
        call(acquittal,"judge",new Class<?>[]{LivingEntity.class},cow);
        check(acquittal.isClosing()&&acquittal.verdict()==JudgmentRules.ACQUITTED&&cow.getHealth()==cow.getMaxHealth(),"innocent not acquitted");
        acquittal.discard();
        // Actual randomized judge eventually exercises both branches without production debug hooks.
        boolean light=false,heavy=false;
        for(int i=0;i<30&&(!light||!heavy);i++){
            var trial=new JudgmentDomainEntity(level,owner,enemy);level.addFreshEntity(trial);
            call(trial,"judge",new Class<?>[]{LivingEntity.class},enemy);
            if(trial.verdict()==JudgmentRules.CONFISCATION){
                var effect=enemy.getEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
                var slow=enemy.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                check(effect!=null&&effect.getAmplifier()==2&&effect.getDuration()==600,"weakness III/30s");
                check(slow!=null&&slow.getAmplifier()==2&&slow.getDuration()==600,"slowness III/30s");light=true;
            }else heavy=true;
            trial.discard();
        }
        check(light&&heavy,"both random verdicts missing");enemy.removeAllEffects();
        var killing=new JudgmentDomainEntity(level,owner,enemy);level.addFreshEntity(killing);
        death(killing,enemy,owner);check(!enemy.isAlive(),"ordinary hostile execution");killing.discard();
        var reduction=new JudgmentDomainEntity(level,owner,boss);level.addFreshEntity(reduction);
        float max=boss.getMaxHealth();death(reduction,boss,owner);
        check(Math.abs(boss.getMaxHealth()-max*.7)<.01,"boss 30% reduction");
        death(reduction,boss,owner);check(Math.abs(boss.getMaxHealth()-max*.7)<.01,"verdict applied twice");
        reduction.discard();
        var saved=new net.minecraft.nbt.CompoundTag();boss.saveWithoutId(saved);
        var reloaded=EntityType.WARDEN.create(level);reloaded.load(saved);
        check(Math.abs(reloaded.getMaxHealth()-max*.7)<.01,"boss max HP modifier not persistent");
        owner.discard();enemy.discard();boss.discard();cow.discard();
        // Actual ServerPlayer damage path, including armour reduction; no maximum-health mutation.
        p.setHealth(p.getMaxHealth());p.invulnerableTime=0;
        var plain=new JudgmentDomainEntity(level,p,p);level.addFreshEntity(plain);
        playerMax=p.getMaxHealth();death(plain,p,p);plain.discard();
        plainLoss=playerMax-p.getHealth();check(Math.abs(plainLoss-playerMax*.6)<.01,"unarmoured player 60%: "+plainLoss);
        p.setHealth(playerMax);p.invulnerableTime=0;
        p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.DIAMOND_HELMET));
        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
        p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.DIAMOND_LEGGINGS));
        p.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.DIAMOND_BOOTS));
    }
    private static void armourCheck(ServerLevel level,ServerPlayer p){
        // Equipment attributes are applied by vanilla on the following entity tick.
        check(p.getArmorValue()==20,"diamond armour attributes not applied");
        p.invulnerableTime=0;
        var armour=new JudgmentDomainEntity(level,p,p);level.addFreshEntity(armour);
        death(armour,p,p);armour.discard();
        float armouredLoss=playerMax-p.getHealth();
        check(armouredLoss>0&&armouredLoss<plainLoss,"player armour did not mitigate: "+armouredLoss);
        check(p.getMaxHealth()==playerMax,"player maximum health changed");
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})p.setItemSlot(slot,ItemStack.EMPTY);
        p.setHealth(playerMax);p.invulnerableTime=0;
        TrueNinsMod.LOGGER.info("JUDGMENT_CHECK: acquittal, both verdicts, III/600 ticks, execution, boss persistence, once-only, player damage={} armour={} maxHP={} passed",plainLoss,armouredLoss,playerMax);
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!Boolean.getBoolean("truenins.judgmentCheck")||!ready||e.phase!=TickEvent.Phase.END)return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        ServerPlayer p=server.getPlayerList().getPlayers().get(0);ServerLevel level=p.serverLevel();++tick;
        if(tick==10)rules(level,p);
        if(tick==12){armourCheck(level,p);checked=true;}
        if(tick==40){
            for(Entity entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))
                if(entity instanceof net.minecraft.world.entity.item.ItemEntity || entity instanceof ExperienceOrb) entity.discard();
            p.teleportTo(level,0,120,-4,0,0);
            defendant=zombie(level,0,4);defendant.setHealth(20);bystander=zombie(level,6,-3);
            outerBystander=zombie(level,20,-3);
            initial=defendant.position();
            // Verify eye selection, obstruction, and overlap lock using the actual targeting code.
            p.setXRot(0);p.setYRot(0);
            check(JudgmentTargeting.pick(p,24)==defendant,"single visible eye-ray target");
            level.setBlock(new BlockPos(0,121,0),Blocks.STONE.defaultBlockState(),3);
            check(JudgmentTargeting.pick(p,24)==null,"selected through a wall");
            level.setBlock(new BlockPos(0,121,0),Blocks.AIR.defaultBlockState(),3);
            if(net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) JudgmentIronChecks.start(p,defendant);
            else { court=new JudgmentDomainEntity(level,p,defendant);level.addFreshEntity(court); }
        }
        if(tick==44){
            if(net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) court=JudgmentIronChecks.completed(p,defendant);
            check(!JudgmentTargeting.available(p,defendant),"overlap permitted");
        }
        if(tick==80){
            check(JudgmentControl.isAffected(p)&&JudgmentControl.isAffected(defendant)&&JudgmentControl.isAffected(bystander),"control missed participant/bystander");
            check(JudgmentControl.isAffected(outerBystander),"expanded outer ring control missed");
            check(entranceSound,"entrance gavel did not reach sound engine");
            check(!bystander.isNoAi(),"NoAI NBT changed");
            Vec3 before=bystander.position();bystander.move(MoverType.SELF,new Vec3(3,1,0));
            check(before.distanceToSqr(bystander.position())<.0001,"movement mixin failed");
            // Server packet handler must accept look while rejecting displacement.
            p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(4,124,-10,37,15,true));
            check(p.getYRot()==37&&p.getXRot()==15,"look blocked");
            check(p.position().distanceToSqr(court.stand(false))<.0001,"packet position accepted");
            p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(0,0,true));
            TrueNinsMod.LOGGER.info("JUDGMENT_CHECK: ray, wall, overlap, control, AI flag, movement and server look/move packet checks passed");
            // World obstacles must disappear from the court capture, then return unchanged.
            for(int x=-3;x<=3;x++)for(int y=120;y<=126;y++)level.setBlock(new BlockPos(x,y,0),Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),3);
            for(int y=120;y<130;y++)level.setBlock(new BlockPos(5,y,2),Blocks.OAK_LOG.defaultBlockState(),3);
        }
        // Lock the visual preview after the real randomized verdict, but before its close timer.
        // Applying it before verdict tick lets judge() overwrite it later in the same trial.
        if(tick==220){
            if(court.verdict()==JudgmentRules.CONFISCATION){
                // The judge method marks light verdict resolved; reset only in this preview harness.
                try{Field f=JudgmentDomainEntity.class.getDeclaredField("resolved");f.setAccessible(true);f.setBoolean(court,false);}
                catch(Exception ex){throw new AssertionError(ex);}
            }
            forceDeath(court);
        }
        if(tick==335){
            check(!court.isAlive(),"domain did not clean up");
            check(!JudgmentControl.isAffected(p)&&!JudgmentControl.isAffected(bystander)&&!JudgmentControl.isAffected(outerBystander),"control did not release");
            Vec3 before=bystander.position();bystander.move(MoverType.SELF,new Vec3(.5,0,0));
            check(before.distanceToSqr(bystander.position())>.01,"movement not restored");
            check(p.position().distanceToSqr(new Vec3(0,120,-4))<1,"caster not returned");
            check(originalChest.equals(level.getBlockEntity(CHEST).saveWithFullMetadata()),"container NBT changed");
            check(level.getBlockState(new BlockPos(0,123,0)).is(Blocks.MOSSY_STONE_BRICKS),"projected court changed obstacle wall");
            for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)if(x!=3||z!=3)
                check(level.getBlockState(new BlockPos(x,119,z)).is(Blocks.STONE),"original floor changed");
            TrueNinsMod.LOGGER.info("JUDGMENT_CHECK: cleanup, position recovery, AI/movement release, original blocks and container NBT passed; complete={}",checked);
        }
        if(tick==340){
            Zombie other=zombie(level,0,4);var abort=new JudgmentDomainEntity(level,p,other);level.addFreshEntity(abort);
            for(int i=0;i<24;i++)abort.tick();
            check(JudgmentControl.isAffected(p),"abort setup");
            JudgmentControl.logout(new PlayerEvent.PlayerLoggedOutEvent(p));
            check(!abort.isAlive()&&!JudgmentControl.isAffected(p)&&!JudgmentControl.isAffected(other),"logout handler did not release");
            check(p.position().distanceToSqr(new Vec3(0,120,-4))<1,"abort return");other.discard();
            TrueNinsMod.LOGGER.info("JUDGMENT_CHECK: participant logout handler immediately removes court, releases control and returns remaining participant passed");
        }
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){
        if(!Boolean.getBoolean("truenins.judgmentCheck")||e.phase!=TickEvent.Phase.END||!ready)return;
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.options.renderDistance().set(6);
        mc.player.setYRot(0);mc.player.yRotO=0;
        float pitch=tick<210?8:-2;mc.player.setXRot(pitch);mc.player.xRotO=pitch;
        if(tick>=175&&tick<183 || tick>=185&&tick<200 || tick>=210&&tick<225){
            if(camera==null)camera=new net.minecraft.world.entity.decoration.ArmorStand(mc.level,0,140,-.1);
            boolean detail=tick<183,overhead=!detail&&tick<200;
            double x=detail?0:overhead?0:35,y=detail?122.7:overhead?139:128,z=detail?4.5:overhead?-.1:-35;
            camera.setPos(x,y,z);camera.xo=x;camera.yo=y;camera.zo=z;
            camera.setYRot(detail?0:overhead?0:45);camera.yRotO=camera.getYRot();
            camera.setXRot(detail?-5:overhead?90:11);camera.xRotO=camera.getXRot();
            mc.options.fov().set(overhead?105:70);mc.setCameraEntity(camera);
        } else {mc.setCameraEntity(mc.player);mc.options.fov().set(70);}
        // The first few frames prove that the black intrusion grows from a direction,
        // before the courtroom and its lights are allowed to emerge.
        int[] shots={45,47,49,51,54,56,60,80,102,116,135,165,178,190,218,234,246,252,258,274,292,328};
        for(int target:shots)if(tick>=target&&shot<target){
            shot=target;Screenshot.grab(mc.gameDirectory,"255-court-"+target+".png",mc.getMainRenderTarget(),c->TrueNinsMod.LOGGER.info("JUDGMENT screenshot: {}",c.getString()));break;
        }
        if(tick>=355)mc.stop();
    }
}
