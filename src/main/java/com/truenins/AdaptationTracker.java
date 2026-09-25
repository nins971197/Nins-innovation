package com.truenins;

import net.minecraft.world.entity.LivingEntity;
import java.util.*;

public final class AdaptationTracker {

    private static final Map<UUID, Map<String, AdaptationEntry>> DATA = new HashMap<>();

    public static final class AdaptationEntry {
        int hitCount;
        long lastHitTick;
    }

    private AdaptationTracker() {}

    public static float recordAndGet(LivingEntity entity, String damageMsgId, long gameTime) {
        return recordAndGet(entity, damageMsgId, gameTime,
            TrueNinsConfig.ADAPTATION_REDUCTION_PERCENT.get());
    }

    public static float recordAndGet(LivingEntity entity, String damageMsgId, long gameTime, double reduction) {
        int threshold = TrueNinsConfig.ADAPTATION_HIT_THRESHOLD.get();
        int resetTicks = TrueNinsConfig.ADAPTATION_RESET_TICKS.get();

        UUID id = entity.getUUID();
        Map<String, AdaptationEntry> map = DATA.computeIfAbsent(id, k -> new HashMap<>());
        AdaptationEntry entry = map.computeIfAbsent(damageMsgId, k -> new AdaptationEntry());

        if (gameTime - entry.lastHitTick > resetTicks) {
            entry.hitCount = 0;
        }

        entry.hitCount++;
        entry.lastHitTick = gameTime;

        return entry.hitCount >= threshold ? (float) reduction : 1.0F;
    }

    public static void clean(LivingEntity entity) {
        DATA.remove(entity.getUUID());
    }
}
