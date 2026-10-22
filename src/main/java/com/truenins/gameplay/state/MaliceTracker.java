package com.truenins.gameplay.state;

import com.truenins.TrueNinsConfig;
import com.truenins.gameplay.tag.TrueninsChannels;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Malice grows a multiplier on repeated hits. Each channel builds its own stack
 * count, and the caller uses the strongest channel multiplier so two sources can
 * never multiply the same hit twice.
 */
public final class MaliceTracker {

    private static final Map<UUID, Map<UUID, State>> NBT_DATA = new HashMap<>();
    private static final Map<UUID, Map<UUID, State>> ENCHANT_DATA = new HashMap<>();

    private static final class State {
        int hitCount;
        long lastHitTick;
        float lastDamage;
    }

    private MaliceTracker() {}

    public static float getMultiplier(LivingEntity attacker, LivingEntity target, long now, float baseDamage,
                                      int channel) {
        Map<UUID, State> perTarget = bucket(channel)
            .computeIfAbsent(attacker.getUUID(), key -> new HashMap<>());
        State state = perTarget.computeIfAbsent(target.getUUID(), key -> new State());

        long resetTicks = TrueNinsConfig.MALICE_RESET_SECONDS.get() * 20L;
        if (now - state.lastHitTick > resetTicks) {
            state.hitCount = 0;
            state.lastDamage = 0.0F;
        }

        state.hitCount++;
        state.lastHitTick = now;

        int threshold = TrueNinsConfig.MALICE_THRESHOLD.get();
        float percent = TrueNinsConfig.MALICE_PERCENT.get().floatValue();
        if (state.hitCount <= threshold) {
            state.lastDamage = baseDamage;
            return 1.0F;
        }

        float previous = state.lastDamage > 0.0F ? state.lastDamage : baseDamage;
        float boosted = previous * (1.0F + percent);
        state.lastDamage = boosted;
        return baseDamage > 0.0F ? boosted / baseDamage : 1.0F;
    }

    public static void clean(UUID attackerId) {
        NBT_DATA.remove(attackerId);
        ENCHANT_DATA.remove(attackerId);
    }

    private static Map<UUID, Map<UUID, State>> bucket(int channel) {
        return channel == TrueninsChannels.ENCHANT ? ENCHANT_DATA : NBT_DATA;
    }
}
