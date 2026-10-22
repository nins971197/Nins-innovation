package com.truenins.gameplay.state;

import com.truenins.TrueNinsConfig;
import com.truenins.gameplay.tag.TrueninsChannels;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mitigation caps the damage a target takes per tick. Each channel owns its own
 * per-tick budget, so two sources are capped one after the other instead of
 * sharing the same allowance.
 */
public final class MitigationTracker {

    private static final Map<UUID, State> NBT_DATA = new HashMap<>();
    private static final Map<UUID, State> ENCHANT_DATA = new HashMap<>();

    private static final class State {
        float damageThisTick;
        long tick;
    }

    private MitigationTracker() {}

    public static float applyCap(LivingEntity entity, float amount, long currentTick, int channel) {
        Map<UUID, State> data = channel == TrueninsChannels.ENCHANT ? ENCHANT_DATA : NBT_DATA;
        State state = data.computeIfAbsent(entity.getUUID(), key -> new State());

        if (currentTick != state.tick) {
            state.damageThisTick = 0.0F;
            state.tick = currentTick;
        }

        float maxHealth = entity.getMaxHealth();
        if (maxHealth <= 0.0F) return amount;

        float baseCap = TrueNinsConfig.MITIGATION_BASE_CAP.get().floatValue();
        float scale = TrueNinsConfig.MITIGATION_SCALE.get().floatValue();
        float healthPercent = entity.getHealth() / maxHealth;
        float cap = Math.max(baseCap - (1.0F - healthPercent) * baseCap * scale, baseCap * 0.4F);

        float remaining = cap - state.damageThisTick;
        if (remaining <= 0.0F) return 0.0F;

        float allowed = Math.min(amount, remaining);
        state.damageThisTick += allowed;
        return allowed;
    }

    public static void clean(UUID id) {
        NBT_DATA.remove(id);
        ENCHANT_DATA.remove(id);
    }
}
