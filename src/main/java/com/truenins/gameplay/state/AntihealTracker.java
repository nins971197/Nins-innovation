package com.truenins.gameplay.state;

import com.truenins.gameplay.tag.TrueninsChannels;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Anti heal entries live per channel: each source keeps its own duration and
 * reduction, and the healing that is actually allowed uses the strongest entry.
 */
public final class AntihealTracker {

    private static final Map<UUID, State> NBT_DATA = new HashMap<>();
    private static final Map<UUID, State> ENCHANT_DATA = new HashMap<>();

    /** Set while {@code heal} is re-routed through {@code setHealth}; it must not be scaled twice. */
    public static final ThreadLocal<Boolean> FROM_HEAL = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final class State {
        long untilTick;
        float reduction;

        State(long untilTick, float reduction) {
            this.untilTick = untilTick;
            this.reduction = reduction;
        }
    }

    private AntihealTracker() {}

    public static void apply(UUID targetId, long now, int durationSeconds, float reduction, int channel) {
        Map<UUID, State> data = bucket(channel);
        long until = now + durationSeconds * 20L;
        State existing = data.get(targetId);
        if (existing != null && existing.untilTick > now) {
            reduction = Math.max(reduction, existing.reduction);
            until = Math.max(until, existing.untilTick);
        }
        data.put(targetId, new State(until, reduction));
    }

    public static float getReduction(LivingEntity entity) {
        long now = entity.level().getGameTime();
        float reduction = 0.0F;
        reduction = Math.max(reduction, reduction(NBT_DATA, entity.getUUID(), now));
        reduction = Math.max(reduction, reduction(ENCHANT_DATA, entity.getUUID(), now));
        return reduction;
    }

    private static float reduction(Map<UUID, State> data, UUID id, long now) {
        State state = data.get(id);
        if (state == null) return 0.0F;
        if (now > state.untilTick) {
            data.remove(id);
            return 0.0F;
        }
        return state.reduction;
    }

    public static void clean(UUID id) {
        NBT_DATA.remove(id);
        ENCHANT_DATA.remove(id);
    }

    private static Map<UUID, State> bucket(int channel) {
        return channel == TrueninsChannels.ENCHANT ? ENCHANT_DATA : NBT_DATA;
    }
}
