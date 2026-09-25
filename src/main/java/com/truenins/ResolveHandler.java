package com.truenins;

import net.minecraft.world.entity.LivingEntity;
import java.util.*;

public final class ResolveHandler {

    public static final int LAST_STAND_TICKS = 200;

    public static final float LAST_STAND_END_DAMAGE = 0.5F;

    public static class ResolveState {
        long startTick;
        boolean active;
        long cooldownUntil;
        int durationTicks;
        float endDamagePercent;

        boolean enforceEndDamage;
    }

    private static final Map<UUID, ResolveState> DATA = new HashMap<>();
    private ResolveHandler() {}

    public static boolean tryTrigger(LivingEntity entity, long now) {
        return tryTrigger(entity, now,
            TrueNinsConfig.RESOLVE_DURATION_SECONDS.get() * 20,
            TrueNinsConfig.RESOLVE_DAMAGE_PERCENT.get().floatValue(),
            false);
    }

    public static boolean tryTrigger(LivingEntity entity, long now, int durationTicks, float endDamagePercent) {
        return tryTrigger(entity, now, durationTicks, endDamagePercent, true);
    }

    private static boolean tryTrigger(LivingEntity entity, long now, int durationTicks,
                                      float endDamagePercent, boolean enforceEndDamage) {
        ResolveState st = DATA.computeIfAbsent(entity.getUUID(), k -> new ResolveState());
        if (st.active || now < st.cooldownUntil) return false;
        st.active = true;
        st.startTick = now;
        st.durationTicks = durationTicks;
        st.endDamagePercent = endDamagePercent;
        st.enforceEndDamage = enforceEndDamage;
        entity.setHealth(1.0f);
        entity.deathTime = 0;
        entity.hurtTime = 0;
        return true;
    }

    public static void tick(LivingEntity entity, long now) {
        ResolveState st = DATA.get(entity.getUUID());
        if (st == null || !st.active) return;
        if (now - st.startTick >= st.durationTicks) {

            if (st.enforceEndDamage) st.active = false;

            float dmg = entity.getMaxHealth() * st.endDamagePercent;
            if (dmg > 0 && entity.getHealth() > 0) {
                entity.invulnerableTime = 0;
                entity.hurt(entity.damageSources().generic(), dmg);
            }

            st.active = false;
            st.cooldownUntil = now + TrueNinsConfig.RESOLVE_COOLDOWN_SECONDS.get() * 20L;
        }
    }

    public static int getRemainingTicks(LivingEntity entity, long now) {
        ResolveState st = DATA.get(entity.getUUID());
        if (st == null || !st.active) return 0;
        return Math.max(0, (int)(st.durationTicks - (now - st.startTick)));
    }

    public static boolean isActive(LivingEntity entity) { ResolveState st = DATA.get(entity.getUUID()); return st != null && st.active; }
    public static boolean isInvulnerable(LivingEntity entity) { return isActive(entity); }
    public static void clean(UUID id) { DATA.remove(id); }
}
