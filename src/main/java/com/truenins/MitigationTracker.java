package com.truenins;

import net.minecraft.world.entity.LivingEntity;
import java.util.*;

public final class MitigationTracker {

    private static final Map<UUID, MitigationState> DATA = new HashMap<>();

    public static final class MitigationState {
        float damageThisTick;
        long tick;
    }

    private MitigationTracker() {}

    public static float applyCap(LivingEntity entity, float amount, long currentTick) {
        UUID id = entity.getUUID();

        MitigationState state = DATA.get(id);
        if (state == null) {
            state = new MitigationState();
            DATA.put(id, state);
        }

        if (currentTick != state.tick) {
            state.damageThisTick = 0;
            state.tick = currentTick;
        }

        float maxHp = entity.getMaxHealth();
        if (maxHp <= 0f) return amount;

        float baseCap = TrueNinsConfig.MITIGATION_BASE_CAP.get().floatValue();
        float scale = TrueNinsConfig.MITIGATION_SCALE.get().floatValue();
        float healthPct = entity.getHealth() / maxHp;
        float cap = Math.max(baseCap - (1f - healthPct) * baseCap * scale, baseCap * 0.4f);

        float remaining = cap - state.damageThisTick;
        if (remaining <= 0f) return 0f;

        float allowed = Math.min(amount, remaining);
        state.damageThisTick += allowed;
        return allowed;
    }

    public static void clean(LivingEntity entity) {
        DATA.remove(entity.getUUID());
    }
}
