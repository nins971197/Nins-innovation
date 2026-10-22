package com.truenins.gameplay.state;

import com.truenins.TrueNinsConfig;
import com.truenins.gameplay.tag.TrueninsChannels;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptation keeps one stack entry per damage message and channel, so the NBT
 * marker and the same-name enchantment build their stacks on independent timers.
 */
public final class AdaptationTracker {

    private static final Map<UUID, Map<String, Entry>> NBT_DATA = new HashMap<>();
    private static final Map<UUID, Map<String, Entry>> ENCHANT_DATA = new HashMap<>();

    private static final class Entry {
        int hitCount;
        long lastHitTick;
    }

    private AdaptationTracker() {}

    public static float recordAndGet(LivingEntity entity, String damageMsgId, long gameTime, int channel) {
        return recordAndGet(entity, damageMsgId, gameTime, channel,
            TrueNinsConfig.ADAPTATION_REDUCTION_PERCENT.get().floatValue());
    }

    /**
     * @return the damage multiplier for this hit: {@code 1.0} until the stack
     *         threshold is reached, then the channel parameter.
     */
    public static float recordAndGet(LivingEntity entity, String damageMsgId, long gameTime, int channel,
                                     float multiplier) {
        int threshold = TrueNinsConfig.ADAPTATION_HIT_THRESHOLD.get();
        int resetTicks = TrueNinsConfig.ADAPTATION_RESET_TICKS.get();
        if (threshold <= 0) return 1.0F;

        Map<String, Entry> perMessage = bucket(channel)
            .computeIfAbsent(entity.getUUID(), key -> new HashMap<>());
        String messageKey = damageMsgId == null ? "" : damageMsgId;
        Entry entry = perMessage.computeIfAbsent(messageKey, key -> new Entry());

        if (gameTime - entry.lastHitTick > resetTicks) entry.hitCount = 0;
        entry.hitCount++;
        entry.lastHitTick = gameTime;

        return entry.hitCount >= threshold ? multiplier : 1.0F;
    }

    public static void clean(UUID id) {
        NBT_DATA.remove(id);
        ENCHANT_DATA.remove(id);
    }

    private static Map<UUID, Map<String, Entry>> bucket(int channel) {
        return channel == TrueninsChannels.ENCHANT ? ENCHANT_DATA : NBT_DATA;
    }
}
