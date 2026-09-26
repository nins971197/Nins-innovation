package com.truenins.client;

import com.truenins.TrueNinsMod;
import com.truenins.register.TNMobEffects;
import com.truenins.register.TNParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT)
public final class InfinityStarAura {

    private static final int EMIT_EVERY = 2;
    private static final int BURST_EVERY = 34;
    private static final double RANGE = 28.0D;
    private static final double R_MIN = 0.58D;
    private static final double R_MAX = 0.98D;
    private static final double SPIN = 0.028D;
    private static final double TWIST = 2.8D;
    private static final double STAR_CHANCE = 7.0D;

    private InfinityStarAura() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;

        for (AbstractClientPlayer player : mc.level.players()) {
            if (!player.hasEffect(TNMobEffects.INFINITY_FIELD.get())) continue;
            if (player.distanceToSqr(mc.player) > RANGE * RANGE) continue;

            int tick = player.tickCount;
            if (tick % EMIT_EVERY == 0) emit(player, mc.level, false);
            if (tick % BURST_EVERY == 0) emit(player, mc.level, true);
        }
    }

    private static void emit(AbstractClientPlayer player, ClientLevel level, boolean star) {
        RandomSource rnd = level.random;
        double cy = player.getY();
        double height = player.getBbHeight();
        double radius = R_MIN + rnd.nextDouble() * (R_MAX - R_MIN);
        double y = cy + 0.16D + rnd.nextDouble() * (height - 0.24D);
        double frac = Mth.clamp((y - cy) / height, 0.0D, 1.0D);

        double strand = rnd.nextInt(3) * (Math.PI * 2.0D / 3.0D);
        double angle = level.getGameTime() * SPIN + strand + (y - cy) * TWIST
            + (rnd.nextDouble() - 0.5D) * 0.55D;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = player.getX() + cos * radius;
        double z = player.getZ() + sin * radius;

        double spin = 0.020D + rnd.nextDouble() * 0.016D;
        double pull = 0.004D + rnd.nextDouble() * 0.008D;
        double vx = -sin * spin - cos * pull;
        double vz = cos * spin - sin * pull;
        double vy = 0.002D + rnd.nextDouble() * 0.011D;

        level.addParticle(pick(rnd, frac, star), x, y, z, vx, vy, vz);
    }

    private static SimpleParticleType pick(RandomSource rnd, double frac, boolean star) {
        if (star || rnd.nextDouble() * 100.0D < STAR_CHANCE) return TNParticles.MOTE_STAR.get();
        double band = frac + (rnd.nextDouble() - 0.5D) * 0.5D;
        if (band < 0.34D) return TNParticles.MOTE_SHARD.get();
        if (band < 0.72D) return TNParticles.MOTE_SPARK.get();
        return TNParticles.MOTE_VEIL.get();
    }
}
