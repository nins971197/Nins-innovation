package com.truenins.gameplay.state;

import com.truenins.TrueNinsConfig;
import com.truenins.gameplay.tag.TrueninsChannels;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stun locks a target in place. A stunned mob has its AI switched off, which is a
 * persisted field: the original value and the restore tick are stored on the mob
 * itself so a chunk reload, an entity reload or a server restart can always give
 * the AI back. Mobs that were already AI-less are not touched.
 */
public final class StunHandler {

    private static final Vector3f RIFT_BLUE = new Vector3f(0.34F, 0.64F, 1.00F);

    private static final String NBT_AI_ORIGINAL = "truenins_stun_noai_original";
    private static final String NBT_AI_RESTORE_AT = "truenins_stun_noai_restore_at";

    private static final Map<UUID, Long> STUNNED_UNTIL = new HashMap<>();
    private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();

    private StunHandler() {}

    public static boolean isOnCooldown(LivingEntity target, long now, int channel) {
        long[] marks = COOLDOWNS.get(target.getUUID());
        if (marks == null) return false;
        long mark = marks[channel];
        return mark != 0L && now - mark < TrueNinsConfig.stunCooldownTicks();
    }

    public static boolean isDenied(Player player) {
        Long until = STUNNED_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    /** Applies one stun for every channel that is off cooldown, then recharges those channels. */
    public static void applyStun(LivingEntity target, long now, int channelMask) {
        int ready = 0;
        for (int channel = 0; channel < 2; channel++) {
            if ((channelMask & TrueninsChannels.bit(channel)) == 0) continue;
            if (isOnCooldown(target, now, channel)) continue;
            ready |= TrueninsChannels.bit(channel);
        }
        if (ready == 0) return;

        hold(target, now, TrueNinsConfig.stunTicks());

        long[] marks = COOLDOWNS.computeIfAbsent(target.getUUID(), key -> new long[2]);
        for (int channel = 0; channel < 2; channel++) {
            if ((ready & TrueninsChannels.bit(channel)) != 0) marks[channel] = now;
        }
    }

    private static void hold(LivingEntity target, long now, int durationTicks) {
        UUID id = target.getUUID();
        long until = Math.max(now + durationTicks, STUNNED_UNTIL.getOrDefault(id, 0L));
        STUNNED_UNTIL.put(id, until);

        if (target instanceof Player player) {
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.closeContainer();
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 255,
                false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, durationTicks, -128,
                false, false, true));
            return;
        }

        if (target instanceof Mob mob) {
            CompoundTag data = mob.getPersistentData();
            if (data.contains(NBT_AI_RESTORE_AT)) {
                // A second stun only extends the window; ownership stays with the first one.
                data.putLong(NBT_AI_RESTORE_AT, until);
                // Something may have cleared the flag mid-window (/data, another mod): re-assert it.
                if (!mob.isNoAi()) mob.setNoAi(true);
            } else if (!mob.isNoAi()) {
                data.putBoolean(NBT_AI_ORIGINAL, false);
                data.putLong(NBT_AI_RESTORE_AT, until);
                mob.setNoAi(true);
            }
        }
    }

    public static void tickFx(LivingEntity entity, long now) {
        UUID id = entity.getUUID();
        Long until = STUNNED_UNTIL.get(id);
        if (until == null) return;
        if (now >= until) {
            STUNNED_UNTIL.remove(id);
            return;
        }
        if ((entity.tickCount & 1) != 0) return;
        if (!(entity.level() instanceof ServerLevel level)) return;
        emit(level, entity, now);
    }

    private static void emit(ServerLevel level, LivingEntity target, long now) {
        RandomSource random = level.random;
        double centerX = target.getX();
        double centerY = target.getY();
        double centerZ = target.getZ();
        double height = target.getBbHeight();
        double phase = now * 0.17D;

        for (int i = 0; i < 6; i++) {
            double radius = 0.42D + random.nextDouble() * 0.38D;
            double y = centerY + 0.20D + random.nextDouble() * (height - 0.26D);
            double angle = phase + i * 1.0472D + y * 2.3D + (random.nextDouble() - 0.5D) * 0.35D;
            level.sendParticles(new DustParticleOptions(RIFT_BLUE, 0.75F),
                centerX + Math.cos(angle) * radius, y, centerZ + Math.sin(angle) * radius,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        for (int i = 0; i < 4; i++) {
            double radius = 0.35D + random.nextDouble() * 0.40D;
            double y = centerY + 0.25D + random.nextDouble() * (height - 0.30D);
            double angle = phase * 1.3D + i * 1.5708D + (random.nextDouble() - 0.5D) * 0.6D;
            level.sendParticles(ParticleTypes.ENCHANT,
                centerX + Math.cos(angle) * radius, y, centerZ + Math.sin(angle) * radius,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** Restores the AI once the recorded stun window has passed. Safe to call every tick. */
    public static void restoreIfExpired(Mob mob, long now) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(NBT_AI_RESTORE_AT)) return;
        if (now < data.getLong(NBT_AI_RESTORE_AT)) return;
        restoreOwnedAi(mob);
    }

    /** Restores the AI unconditionally and drops the record, used on death, unload and cleanup. */
    public static void restoreOwnedAi(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(NBT_AI_RESTORE_AT) && !data.contains(NBT_AI_ORIGINAL)) return;
        boolean original = data.getBoolean(NBT_AI_ORIGINAL);
        data.remove(NBT_AI_ORIGINAL);
        data.remove(NBT_AI_RESTORE_AT);
        mob.setNoAi(original);
    }

    public static void clean(LivingEntity entity) {
        if (entity instanceof Mob mob) restoreOwnedAi(mob);
        clean(entity.getUUID());
    }

    /** Drops the running window only; the per-channel recharge is kept, used when an entity
     *  leaves the level (a chunk unload must not hand out a free re-stun). */
    public static void cleanActive(UUID id) {
        STUNNED_UNTIL.remove(id);
    }

    public static void clean(UUID id) {
        STUNNED_UNTIL.remove(id);
        COOLDOWNS.remove(id);
    }
}
