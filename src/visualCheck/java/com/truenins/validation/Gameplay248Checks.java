package com.truenins.validation;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsMod;
import com.truenins.entity.domain.ShinganDomainEntity;
import com.truenins.entity.domain.VoidDomainEntity;
import com.truenins.entity.orb.PurpleOrbEntity;
import com.truenins.spell.domain.VoidControl;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Assertions run on real integrated-server entities, in the copied VisualCheck world only. */
@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID)
public final class Gameplay248Checks {
    private static int tick;
    private static Zombie enemy, immuneOwner, purpleTarget;
    private static Zombie firstBarrageTarget, secondBarrageTarget;
    private static VoidDomainEntity voidDomain;
    private static PurpleOrbEntity purple;
    private static ShinganDomainEntity barrage;
    private static float hp, maxHp, struckHp;
    private static final Vec3 BLAST = new Vec3(-25.5, 125.5, 20.5);
    private static boolean initialBreakBlocks;
    private static int purpleHits;

    @SubscribeEvent public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (Boolean.getBoolean("truenins.visualCheck") && event.getEntity() == purpleTarget
                && event.getSource().getDirectEntity() instanceof PurpleOrbEntity) purpleHits++;
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if ((Boolean.getBoolean("truenins.judgmentCheck") || Boolean.getBoolean("truenins.conflagrationCheck"))) return;
        if (!Boolean.getBoolean("truenins.visualCheck") || event.phase != TickEvent.Phase.END) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) return;
        ServerLevel level = server.overworld();
        ServerPlayer p = server.getPlayerList().getPlayers().get(0);
        tick++;
        if (barrage != null && barrage.isAlive()) {
            float expansion = barrage.transition(.5f);
            check(expansion >= 0f && expansion <= 1f, "domain transition out of bounds: " + expansion);
        }
        if (firstBarrageTarget != null && tick < 267) anchor(firstBarrageTarget, 24, 124, 20);
        if (secondBarrageTarget != null && tick < 267) anchor(secondBarrageTarget, 25, 124, 20);
        if (tick == 25) {
            initialBreakBlocks = TrueNinsConfig.purpleBreakBlocks();
            enemy = zombie(level, 8, 121, -8, false);
            immuneOwner = zombie(level, 10, 121, -8, false);
            immuneOwner.setNoAi(true);
            voidDomain = new VoidDomainEntity(level, immuneOwner, 30, 6);
            level.addFreshEntity(voidDomain);
            hp = enemy.getHealth(); maxHp = enemy.getMaxHealth();
        }
        if (tick == 28) {
            check(VoidControl.isAffected(enemy), "hostile not controlled");
            check(!VoidControl.isAffected(immuneOwner), "caster controlled");
            check(!enemy.isNoAi(), "NoAI permanently changed");
            Vec3 before = enemy.position();
            enemy.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(1,0,1));
            check(enemy.position().distanceToSqr(before) < .000001, "entity movement not blocked");
        }
        if (tick == 50) {
            check(enemy.getHealth() <= hp && enemy.getMaxHealth() < maxHp,
                "void max-health reduction missing");
        }
        if (tick == 58) {
            check(!VoidControl.isAffected(enemy), "void control failed to expire");
            check(!enemy.isNoAi(), "AI state not restored");
            check(enemy.getMaxHealth() < maxHp, "void max-health reduction missing");
        }
        if (tick == 65) {
            check(enemy.getMaxHealth() < maxHp, "void max-health modifier was incorrectly restored");
            enemy.discard(); immuneOwner.discard();
            TrueNinsMod.LOGGER.info("CHECK250: void permanently reduced max-health by 10%; hostile movement locked and caster exempt");
            barrage = new ShinganDomainEntity(level, p, 200, 6, 30);
            barrage.setPos(24, 124, 20);
            firstBarrageTarget = zombie(level, 24, 124, 20, true);
            level.addFreshEntity(barrage);
            prepareBlast(level);
            TrueNinsConfig.PURPLE_BREAK_BLOCKS.set(false);
            purpleTarget = zombie(level, -21.5, 125, 20, false);
            purpleTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
            purpleTarget.setHealth(200);
            purple = new PurpleOrbEntity(level, p);
            purple.setPos(BLAST.x, BLAST.y, BLAST.z);
            level.addFreshEntity(purple);
        }
        if (tick > 65 && tick < 97) check(purple.position().distanceToSqr(BLAST) < .000001, "Purple moved away from fusion point");
        if (tick == 102) {
            TrueNinsMod.LOGGER.info("CHECK248: purple target health={} max={} dirt={}", purpleTarget.getHealth(), purpleTarget.getMaxHealth(), countDirt(level));
            check(countDirt(level) == 125, "terrain off still broke blocks");
            check(purpleTarget.getHealth() < 200 && purpleTarget.getHealth() >= 155, "Purple damage duplicated or missing");
            check(purpleHits == 1, "Purple hit count " + purpleHits);
            struckHp = purpleTarget.getHealth();
        }
        if (tick == 119) {
            check(purpleTarget.getHealth() == struckHp, "Purple repeated damage");
            purpleTarget.discard();
            TrueNinsConfig.PURPLE_BREAK_BLOCKS.set(true);
            purple = new PurpleOrbEntity(level, p);
            purple.setPos(BLAST.x, BLAST.y, BLAST.z);
            level.addFreshEntity(purple);
        }
        if (tick == 155) {
            check(countDirt(level) < 125, "terrain on did not destroy blocks");
            TrueNinsConfig.PURPLE_BREAK_BLOCKS.set(initialBreakBlocks);
            secondBarrageTarget = zombie(level, 25, 124, 20, true);
            TrueNinsMod.LOGGER.info("CHECK248: Purple stationary; configured damage applied once; terrain off preserved 125 blocks, terrain on destroyed blocks");
        }
        if (tick == 267) {
            check(firstBarrageTarget.isAlive() && secondBarrageTarget.isAlive(), "barrage fixture lost a target");
            check(barrage.attackBudget() == 32, "wrong budget " + barrage.attackBudget());
            check(barrage.attacksFired() == 32, "wrong count " + barrage.attacksFired()+" remaining="+barrage.remainingTicks()+" alive="+barrage.isAlive());
            TrueNinsMod.LOGGER.info("CHECK248: Shingan fired exactly 32 attacks: base 30 plus two distinct living targets, including a later arrival");
            for (var e : com.google.common.collect.ImmutableList.copyOf(level.getAllEntities())) if (e instanceof Zombie && e.getX() > 15) e.discard();
        }
    }
    private static Zombie zombie(ServerLevel level, double x, double y, double z, boolean invulnerable) {
        Zombie zmb = EntityType.ZOMBIE.create(level);
        zmb.setPos(x,y,z); zmb.setNoGravity(true); zmb.setSilent(true); zmb.setNoAi(true);
        zmb.setInvulnerable(invulnerable);
        zmb.setPersistenceRequired();
        // Third-party spell damage need not honour vanilla's invulnerable flag.
        // The budget fixture needs two available targets throughout the full trial.
        if(invulnerable) {
            zmb.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100000);
            zmb.setHealth(100000);
        }
        zmb.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET));
        level.addFreshEntity(zmb);
        if (x > 0 && x < 15) zmb.setNoAi(false);
        return zmb;
    }
    private static void anchor(Zombie zmb, double x, double y, double z) {
        zmb.setPos(x, y, z);
        zmb.setDeltaMovement(Vec3.ZERO);
    }
    private static void prepareBlast(ServerLevel level) {
        for (int x=-2;x<=2;x++) for(int y=-2;y<=2;y++) for(int z=-2;z<=2;z++)
            level.setBlock(BlockPos.containing(BLAST).offset(x,y,z), Blocks.DIRT.defaultBlockState(), 2);
    }
    private static int countDirt(ServerLevel level) {
        int n=0;
        for(int x=-2;x<=2;x++) for(int y=-2;y<=2;y++) for(int z=-2;z<=2;z++)
            if(level.getBlockState(BlockPos.containing(BLAST).offset(x,y,z)).is(Blocks.DIRT)) n++;
        return n;
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
