package com.truenins;

import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public final class AntihealTracker {

    public static final class State {
        long untilTick;
        float reduction;
        State(long untilTick, float reduction) {
            this.untilTick = untilTick;
            this.reduction = reduction;
        }
    }

    private static final Map<UUID, State> DATA = new HashMap<>();

    public static final ThreadLocal<Boolean> FROM_HEAL = ThreadLocal.withInitial(() -> false);

    private AntihealTracker() {}

    public static void apply(UUID targetId, long now, int durationSeconds, float reduction) {
        long until = now + durationSeconds * 20L;
        State existing = DATA.get(targetId);

        if (existing != null && existing.untilTick > now) {
            reduction = Math.max(reduction, existing.reduction);
        }
        DATA.put(targetId, new State(until, reduction));
    }

    public static float getReduction(LivingEntity entity) {
        State s = DATA.get(entity.getUUID());
        if (s == null) return 0f;
        if (entity.level().getGameTime() > s.untilTick) {
            DATA.remove(entity.getUUID());
            return 0f;
        }
        return s.reduction;
    }

    public static void tickCleanup(long now) {
        DATA.entrySet().removeIf(e -> now > e.getValue().untilTick);
    }

    public static void clean(UUID id) {
        DATA.remove(id);
    }
}
