package com.truenins.validation;

import com.truenins.TrueNinsMod;
import com.truenins.TrueNinsConfig;
import com.truenins.entity.domain.ShinganDomainEntity;
import com.truenins.entity.orb.BlueOrbEntity;
import com.truenins.entity.orb.RedOrbEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in harness in a copied development world. Excluded from release jars. */
@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT)
public final class VisualCheckClient {
    private static boolean prepared;
    private static int frames;
    private static volatile int sceneTick;
    private static int lastShot;
    private static boolean savedTerrain;
    private static java.util.List<BlueOrbEntity> flights = new java.util.ArrayList<>();
    private static java.util.List<Vec3> origins = new java.util.ArrayList<>();
    private static java.util.List<Vec3> directions = new java.util.ArrayList<>();
    private static int flightTicks;
    private static MockRed previewRed;
    private static MockBlue previewBlue;
    private static MockBlue occludedBlue;
    private static net.minecraft.world.entity.animal.Cow blastOwner, blastInside, blastOutside;
    private static com.truenins.entity.orb.PurpleOrbEntity blastCheck;
    private static double savedBlastBase, savedBlastScale;
    private static net.minecraft.world.entity.animal.Cow blastDummy(net.minecraft.server.level.ServerLevel level,double x) {
        var cow=net.minecraft.world.entity.EntityType.COW.create(level);
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
        cow.setHealth(100);cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(x,150,0);
        level.addFreshEntity(cow);return cow;
    }
    @SubscribeEvent public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if ((Boolean.getBoolean("truenins.judgmentCheck") || Boolean.getBoolean("truenins.conflagrationCheck"))) return;
        if (!Boolean.getBoolean("truenins.visualCheck") || prepared || !(event.getEntity() instanceof ServerPlayer p)) return;
        prepared = true;
        var level = p.serverLevel();
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
        for (var e : com.google.common.collect.ImmutableList.copyOf(level.getAllEntities())) {
            if (e instanceof net.minecraft.world.entity.LivingEntity && e != p) e.discard();
        }
        for (int x = -42; x <= 42; x++) for (int z = -42; z <= 42; z++) {
            level.setBlock(new BlockPos(x, 119, z), Blocks.STONE.defaultBlockState(), 2);
        }
        p.setGameMode(GameType.CREATIVE);
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(level, 0, 120, -15, 0, -12);
        level.setDayTime(6000);
        ShinganDomainEntity domain = new ShinganDomainEntity(level, p, 72000, 24, 30);
        domain.setPos(0, 120, 0);
        level.addFreshEntity(domain);
        float[][] angles = {{0,0},{45,-35},{-135,40},{0,-90},{0,90},{127,-25},{-27,60}};
        float oldYaw = p.getYRot(), oldPitch = p.getXRot();
        for (int i = 0; i < angles.length; i++) {
            p.setYRot(angles[i][0]); p.setXRot(angles[i][1]);
            BlueOrbEntity orb = new BlueOrbEntity(level, p, 20, false);
            Vec3 origin = new Vec3(8 + i * 2, 200 + i * 3, 8);
            orb.setPos(origin.x, origin.y, origin.z);
            orb.launch();
            level.addFreshEntity(orb);
            flights.add(orb); origins.add(origin); directions.add(p.getLookAngle().normalize());
        }
        p.setYRot(oldYaw); p.setXRot(oldPitch);
        TrueNinsMod.LOGGER.info("VISUAL_CHECK: created reference scene in copied world");
    }

    @SubscribeEvent public static void scene(TickEvent.ServerTickEvent event) {
        if ((Boolean.getBoolean("truenins.judgmentCheck") || Boolean.getBoolean("truenins.conflagrationCheck"))) return;
        if (!Boolean.getBoolean("truenins.visualCheck") || !prepared || event.phase != TickEvent.Phase.END) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) return;
        var level = server.overworld();
        var p = server.getPlayerList().getPlayers().get(0);
        sceneTick++;
        if (sceneTick == 120) {
            for (var e : com.google.common.collect.ImmutableList.copyOf(level.getAllEntities())) if (e instanceof ShinganDomainEntity && e.getX() == 0) e.discard();
            var domain = new ShinganDomainEntity(level, p, 100, 24, 30);
            domain.setPos(0,120,0);
            level.addFreshEntity(domain);
        }
        if (sceneTick == 195) p.teleportTo(level, 0, 120, -36, 0, -12);
        if (sceneTick == 280) {
            for (var e : com.google.common.collect.ImmutableList.copyOf(level.getAllEntities())) if (e instanceof ShinganDomainEntity) e.discard();
            p.teleportTo(level, 0,120,-15,0,-12);
        }
        if (sceneTick == 300) {
            savedTerrain = TrueNinsConfig.purpleBreakBlocks();
            TrueNinsConfig.PURPLE_BREAK_BLOCKS.set(false);
            savedBlastBase=TrueNinsConfig.PURPLE_BLAST_RADIUS.get();savedBlastScale=TrueNinsConfig.PURPLE_BLAST_RADIUS_SCALE.get();
            TrueNinsConfig.PURPLE_BLAST_RADIUS.set(16D);TrueNinsConfig.PURPLE_BLAST_RADIUS_SCALE.set(1.5D);
            blastOwner=blastDummy(level,100);blastInside=blastDummy(level,120);blastOutside=blastDummy(level,126);
            blastCheck=new com.truenins.entity.orb.PurpleOrbEntity(level,blastOwner);
            blastCheck.setPos(100,150,0);level.addFreshEntity(blastCheck);
            var purple = new com.truenins.entity.orb.PurpleOrbEntity(level,p);
            purple.setPos(0,123,-7);
            level.addFreshEntity(purple);
        }
        if(sceneTick==340){
            if(blastInside.getHealth()>=100||blastOutside.getHealth()!=100||blastOwner.getHealth()!=100)
                throw new AssertionError("Purple actual radius/owner exemption: "+blastInside.getHealth()+" / "+blastOutside.getHealth()+" / "+blastOwner.getHealth());
            float after=blastInside.getHealth();
            blastCheck.strikeExplosionTargets(java.util.List.of(blastInside));
            if(blastInside.getHealth()!=after)throw new AssertionError("Purple repeated explosion damage");
            TrueNinsMod.LOGGER.info("PURPLE_CHECK: effective radius={} damages target 20 blocks away (HP={}), target 26 blocks away and caster intact; once-only damage passed",TrueNinsConfig.purpleBlastRadius(),after);
        }
        if (sceneTick == 370) {
            TrueNinsConfig.PURPLE_BREAK_BLOCKS.set(savedTerrain);
            TrueNinsConfig.PURPLE_BLAST_RADIUS.set(savedBlastBase);TrueNinsConfig.PURPLE_BLAST_RADIUS_SCALE.set(savedBlastScale);
            blastCheck.discard();blastOwner.discard();blastInside.discard();blastOutside.discard();
        }
        if (sceneTick == 430 || sceneTick == 460) {
            for (int y=120; y<=124; y++) level.setBlock(new BlockPos(-2,y,-6),
                (sceneTick == 430 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 3);
        }
    }

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || flights.isEmpty()) return;
        if (++flightTicks != 15) return;
        for (int i = 0; i < flights.size(); i++) {
            Vec3 displacement = flights.get(i).position().subtract(origins.get(i));
            Vec3 expected = directions.get(i);
            if (displacement.length() < .1 || displacement.normalize().dot(expected) < .99999) {
                throw new AssertionError("Actual BlueOrbEntity flight lost pitch: " + i + " " + displacement);
            }
            if (displacement.length() > TrueNinsConfig.aoMaxDistance() + .001) throw new AssertionError("Orb exceeded range");
            flights.get(i).discard();
        }
        flights.clear();
        TrueNinsMod.LOGGER.info("VISUAL_CHECK: 7 actual server BlueOrbEntity trajectories passed, including vertical/up/down/diagonal");
    }

    @SubscribeEvent public static void render(TickEvent.RenderTickEvent event) {
        if ((Boolean.getBoolean("truenins.judgmentCheck") || Boolean.getBoolean("truenins.conflagrationCheck"))) return;
        Minecraft mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("truenins.visualCheck") || event.phase != TickEvent.Phase.END
                || mc.level == null || mc.player == null || !prepared) return;
        mc.options.pauseOnLostFocus = false;
        mc.options.hideGui = true;
        mc.options.renderDistance().set(6);
        mc.player.setYRot(0); mc.player.yRotO = 0;
        float pitch = -12;
        mc.player.setXRot(pitch); mc.player.xRotO = pitch;
        ++frames;
        if (lastShot != sceneTick && (sceneTick == 130 || sceneTick == 145 || sceneTick == 180
                || sceneTick == 205 || sceneTick == 225 || sceneTick == 240 || sceneTick == 307 || sceneTick == 320 || sceneTick == 331 || sceneTick == 332 || sceneTick == 333 || sceneTick == 335 || sceneTick == 339 || sceneTick == 345 || sceneTick == 360 || sceneTick == 390 || sceneTick == 450 || sceneTick == 465 || sceneTick == 475 || sceneTick == 490)) {
            lastShot = sceneTick;
            Screenshot.grab(mc.gameDirectory, "255-scene-" + sceneTick + ".png", mc.getMainRenderTarget(),
                message -> TrueNinsMod.LOGGER.info("VISUAL_CHECK screenshot: {}", message.getString()));
        }
        if (sceneTick >= 520) mc.stop();
    }

    @SubscribeEvent public static void clientScene(TickEvent.ClientTickEvent event) {
        if ((Boolean.getBoolean("truenins.judgmentCheck") || Boolean.getBoolean("truenins.conflagrationCheck"))) return;
        Minecraft mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("truenins.visualCheck") || event.phase != TickEvent.Phase.END
                || mc.level == null || mc.player == null) return;
        if (sceneTick >= 160 && sceneTick < 250 && occludedBlue == null) {
            occludedBlue = new MockBlue(mc); occludedBlue.setPos(0,123,38);
            occludedBlue.xo=0; occludedBlue.yo=123; occludedBlue.zo=38;
            mc.level.putNonPlayerEntity(-25103, occludedBlue);
        }
        if (sceneTick >= 250 && occludedBlue != null) { occludedBlue.discard(); occludedBlue = null; }
        if (sceneTick >= 430 && sceneTick < 510 && previewBlue == null) {
            previewBlue = new MockBlue(mc); previewBlue.setPos(-2.8,123,-3);
            previewBlue.xo=-2.8; previewBlue.yo=123; previewBlue.zo=-3;
            mc.level.putNonPlayerEntity(-25101, previewBlue);
            previewRed = new MockRed(mc); previewRed.setPos(2.8,123,-3);
            previewRed.xo=2.8; previewRed.yo=123; previewRed.zo=-3;
            mc.level.putNonPlayerEntity(-25102, previewRed);
        }
    }

    private static final class MockBlue extends BlueOrbEntity {
        MockBlue(Minecraft mc) { super(mc.level, mc.player,20,false); setOrbScale(1); }
    }
    private static final class MockRed extends RedOrbEntity {
        MockRed(Minecraft mc) { super(mc.level,mc.player); setOrbScale(1); }
        @Override public boolean visualFlying() { return sceneTick >= 480; }
        @Override public Vec3 visualDirection() { return new Vec3(.3,.2,1).normalize(); }
        @Override public int explodeProgress() { return -1; }
    }
}


