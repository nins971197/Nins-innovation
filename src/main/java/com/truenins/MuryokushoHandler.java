package com.truenins;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.util.*;

public final class MuryokushoHandler {

    private static final Vector3f RIFT_BLUE = new Vector3f(0.34F, 0.64F, 1.00F);

    private static final Map<UUID, Long> STUNNED_UNTIL = new HashMap<>();

    private static final Map<UUID, Long> AI_RESTORE = new HashMap<>();

    private static final Map<UUID, Long> COOLDOWNS_TAG = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS_ENCH = new HashMap<>();

    private static Map<UUID, Long> cooldowns(int channel) {
        return channel == 1 ? COOLDOWNS_ENCH : COOLDOWNS_TAG;
    }

    private MuryokushoHandler() {}

    public static boolean isOnCooldown(LivingEntity target, long now, int channel) {
        Long last = cooldowns(channel).get(target.getUUID());
        if (last == null) return false;
        return now - last < TrueNinsConfig.muryokushoCooldownTicks();
    }

    public static boolean isDenied(Player player) {
        Long until = STUNNED_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    public static void apply(LivingEntity target, LivingEntity wielder, long now, int durationTicks, int channel) {
        if (durationTicks < 0) durationTicks = TrueNinsConfig.muryokushoTicks();
        long until = now + durationTicks;
        STUNNED_UNTIL.put(target.getUUID(), until);

        if (target instanceof Player player) {
            if (player instanceof ServerPlayer sp) {
                sp.closeContainer();
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 255,
                false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, durationTicks, -128,
                false, false, true));
        } else if (target instanceof Mob mob) {
            mob.setNoAi(true);
            AI_RESTORE.put(mob.getUUID(), until);
        }

        cooldowns(channel).put(target.getUUID(), now);
    }

    public static void tickFx(LivingEntity entity, long now) {
        Long until = STUNNED_UNTIL.get(entity.getUUID());
        if (until == null) return;
        if (now >= until) {
            STUNNED_UNTIL.remove(entity.getUUID());
            return;
        }
        if ((entity.tickCount & 1) != 0) return;
        if (!(entity.level() instanceof ServerLevel level)) return;
        emit(level, entity, now);
    }

    private static void emit(ServerLevel level, LivingEntity target, long now) {
        RandomSource rnd = level.random;
        double cx = target.getX();
        double cy = target.getY();
        double cz = target.getZ();
        double height = target.getBbHeight();
        double base = now * 0.17D;

        for (int i = 0; i < 6; i++) {
            double radius = 0.42D + rnd.nextDouble() * 0.38D;
            double y = cy + 0.20D + rnd.nextDouble() * (height - 0.26D);
            double angle = base + i * 1.0472D + y * 2.3D + (rnd.nextDouble() - 0.5D) * 0.35D;
            level.sendParticles(new DustParticleOptions(RIFT_BLUE, 0.75F),
                cx + Math.cos(angle) * radius, y, cz + Math.sin(angle) * radius,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        for (int i = 0; i < 4; i++) {
            double radius = 0.35D + rnd.nextDouble() * 0.40D;
            double y = cy + 0.25D + rnd.nextDouble() * (height - 0.30D);
            double angle = base * 1.3D + i * 1.5708D + (rnd.nextDouble() - 0.5D) * 0.6D;
            level.sendParticles(ParticleTypes.ENCHANT,
                cx + Math.cos(angle) * radius, y, cz + Math.sin(angle) * radius,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public static void tryRestoreAi(Mob mob, long now) {
        Long restoreAt = AI_RESTORE.get(mob.getUUID());
        if (restoreAt != null && now >= restoreAt) {
            mob.setNoAi(false);
            AI_RESTORE.remove(mob.getUUID());
        }
    }

    public static void clean(UUID uuid) {
        COOLDOWNS_TAG.remove(uuid);
        COOLDOWNS_ENCH.remove(uuid);
        STUNNED_UNTIL.remove(uuid);
        AI_RESTORE.remove(uuid);
    }
}
