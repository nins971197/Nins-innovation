package com.truenins;

import net.minecraft.world.entity.LivingEntity;
import java.util.*;

public final class MaliceTracker {

    public static class MaliceState {
        int hitCount;
        long lastHitTick;
        float lastDamage;
    }

    private static final Map<UUID, Map<UUID, MaliceState>> DATA = new HashMap<>();

    private MaliceTracker() {}

    public static float getMultiplier(LivingEntity attacker, LivingEntity target, long now, float baseDamage) {
        UUID aid = attacker.getUUID();
        UUID tid = target.getUUID();
        Map<UUID, MaliceState> map = DATA.computeIfAbsent(aid, k -> new HashMap<>());
        MaliceState st = map.computeIfAbsent(tid, k -> new MaliceState());

        long cd = TrueNinsConfig.MALICE_RESET_SECONDS.get() * 20L;
        if (now - st.lastHitTick > cd) {
            st.hitCount = 0;
            st.lastDamage = 0;
        }

        st.hitCount++;
        st.lastHitTick = now;

        int threshold = TrueNinsConfig.MALICE_THRESHOLD.get();
        int maxStacks = TrueNinsConfig.MALICE_MAX_STACKS.get();
        float pct = TrueNinsConfig.MALICE_PERCENT.get().floatValue();

        if (st.hitCount <= threshold) {
            st.lastDamage = baseDamage;
            return 1.0f;
        }

        int stacks = Math.min(st.hitCount - threshold, maxStacks);

        float prev = st.lastDamage > 0 ? st.lastDamage : baseDamage;
        float newDmg = prev * (1f + pct);
        st.lastDamage = newDmg;

        return newDmg / baseDamage;
    }

    public static void clean(UUID aid) { DATA.remove(aid); }
}
