package com.truenins;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.*;

public final class JusticeTracker {

    private static final Map<UUID, JusticeState> DATA = new HashMap<>();
    private static final UUID MODIFIER_ID = UUID.fromString("e7a3b1c4-5d2f-4a8e-9b6c-1d3e5f7a9b2c");

    public static class JusticeState {
        float reducedAmount;
        long lastHitTick;
    }

    private JusticeTracker() {}

    public static boolean tryApply(LivingEntity target, long now) {
        UUID id = target.getUUID();
        JusticeState st = DATA.get(id);
        if (st != null && st.lastHitTick > 0) return false;

        float pct = TrueNinsConfig.JUSTICE_PERCENT.get().floatValue();
        float reduction = target.getMaxHealth() * pct;

        AttributeInstance attr = target.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.removeModifier(MODIFIER_ID);
            attr.removeModifier(MODIFIER_ID);
            attr.addTransientModifier(new AttributeModifier(MODIFIER_ID, "justice", -reduction, AttributeModifier.Operation.ADDITION));
        }

        DATA.put(id, new JusticeState());
        DATA.get(id).reducedAmount = reduction;
        DATA.get(id).lastHitTick = now;
        return true;
    }

    public static void tickCheck(LivingEntity entity, long now) {
        UUID id = entity.getUUID();
        JusticeState st = DATA.get(id);
        if (st == null || st.lastHitTick <= 0) return;

        long cdTicks = TrueNinsConfig.JUSTICE_COOLDOWN_SECONDS.get() * 20L;
        if (now - st.lastHitTick >= cdTicks) {
            AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) attr.removeModifier(MODIFIER_ID);
            DATA.remove(id);
        }
    }

    public static void clean(UUID id) {
        DATA.remove(id);
    }
}
