package com.truenins.gameplay.state;

import com.truenins.TrueNinsConfig;
import com.truenins.gameplay.tag.TrueninsChannels;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Last stand: the entity survives on one heart for a while and takes a final hit
 * when the window closes. Both channels share one window, use the longest
 * duration and the mildest final damage, and recharge independently.
 */
public final class ResolveHandler {

    public static final int LAST_STAND_TICKS = 200;

    public static final float LAST_STAND_END_DAMAGE = 0.5F;

    private static final class State {
        long startTick;
        boolean active;
        int durationTicks;
        float endDamagePercent;
        int consumedMask;
        final long[] cooldownUntil = new long[2];
    }

    private static final Map<UUID, State> DATA = new HashMap<>();

    private ResolveHandler() {}

    public static boolean tryTrigger(LivingEntity entity, long now, int channelMask) {
        State state = DATA.computeIfAbsent(entity.getUUID(), key -> new State());
        if (state.active) return false;

        int ready = 0;
        for (int channel = 0; channel < 2; channel++) {
            int bit = TrueninsChannels.bit(channel);
            if ((channelMask & bit) != 0 && now >= state.cooldownUntil[channel]) ready |= bit;
        }
        if (ready == 0) return false;

        int duration = 1;
        float endDamage = 1.0F;
        if ((ready & TrueninsChannels.NBT_BIT) != 0) {
            duration = Math.max(duration, TrueNinsConfig.RESOLVE_DURATION_SECONDS.get() * 20);
            endDamage = Math.min(endDamage, TrueNinsConfig.RESOLVE_DAMAGE_PERCENT.get().floatValue());
        }
        if ((ready & TrueninsChannels.ENCHANT_BIT) != 0) {
            duration = Math.max(duration, LAST_STAND_TICKS);
            endDamage = Math.min(endDamage, LAST_STAND_END_DAMAGE);
        }

        state.active = true;
        state.startTick = now;
        state.durationTicks = duration;
        state.endDamagePercent = endDamage;
        state.consumedMask = ready;

        entity.setHealth(1.0F);
        entity.deathTime = 0;
        entity.hurtTime = 0;
        return true;
    }

    public static void tick(LivingEntity entity, long now) {
        State state = DATA.get(entity.getUUID());
        if (state == null || !state.active) return;
        if (now - state.startTick < state.durationTicks) return;

        state.active = false;

        // Record the recharge *before* the closing hit: the hit can kill, and the death handler
        // starts a new last stand whenever the cooldown still looks ready (double window, no damage).
        long cooldownTicks = TrueNinsConfig.RESOLVE_COOLDOWN_SECONDS.get() * 20L;
        for (int channel = 0; channel < 2; channel++) {
            if ((state.consumedMask & TrueninsChannels.bit(channel)) != 0) {
                state.cooldownUntil[channel] = now + cooldownTicks;
            }
        }

        float damage = entity.getMaxHealth() * state.endDamagePercent;
        if (damage > 0.0F && entity.getHealth() > 0.0F) {
            entity.invulnerableTime = 0;
            entity.hurt(entity.damageSources().generic(), damage);
        }
    }

    public static int getRemainingTicks(LivingEntity entity, long now) {
        State state = DATA.get(entity.getUUID());
        if (state == null || !state.active) return 0;
        return Math.max(0, state.durationTicks - (int) (now - state.startTick));
    }

    public static boolean isActive(LivingEntity entity) {
        State state = DATA.get(entity.getUUID());
        return state != null && state.active;
    }

    public static boolean isInvulnerable(LivingEntity entity) {
        return isActive(entity);
    }

    public static void clean(UUID id) {
        DATA.remove(id);
    }
}
