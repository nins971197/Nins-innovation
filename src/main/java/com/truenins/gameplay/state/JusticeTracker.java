package com.truenins.gameplay.state;

import com.truenins.TrueNinsConfig;
import com.truenins.gameplay.tag.TrueninsChannels;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Justice shrinks the maximum health of a target for a while. Both channels apply
 * their own modifier and expire on their own timer.
 */
public final class JusticeTracker {

    private static final UUID[] MODIFIER_IDS = {
        UUID.fromString("e7a3b1c4-5d2f-4a8e-9b6c-1d3e5f7a9b2c"),
        UUID.fromString("3f0d9c52-7b14-4e6a-8c31-0d5b7a2e9f41")
    };
    private static final String[] MODIFIER_NAMES = {"justice_tag", "justice_enchant"};

    /** Applied tick per channel, 0 when that channel is not active. */
    private static final Map<UUID, long[]> DATA = new HashMap<>();

    private JusticeTracker() {}

    public static boolean tryApply(LivingEntity target, long now, int channelMask) {
        long[] marks = DATA.computeIfAbsent(target.getUUID(), key -> new long[2]);
        AttributeInstance health = target.getAttribute(Attributes.MAX_HEALTH);
        float reduction = target.getMaxHealth() * TrueNinsConfig.JUSTICE_PERCENT.get().floatValue();

        boolean applied = false;
        for (int channel = 0; channel < 2; channel++) {
            if ((channelMask & TrueninsChannels.bit(channel)) == 0) continue;
            if (marks[channel] != 0) continue;
            marks[channel] = now;
            if (health != null && reduction > 0.0F) {
                removeModifier(health, MODIFIER_IDS[channel]);
                health.addTransientModifier(new AttributeModifier(MODIFIER_IDS[channel],
                    MODIFIER_NAMES[channel], -reduction, AttributeModifier.Operation.ADDITION));
            }
            applied = true;
        }
        return applied;
    }

    public static void tickCheck(LivingEntity entity, long now) {
        UUID id = entity.getUUID();
        long[] marks = DATA.get(id);
        if (marks == null) return;

        long cooldownTicks = TrueNinsConfig.JUSTICE_COOLDOWN_SECONDS.get() * 20L;
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        boolean active = false;

        for (int channel = 0; channel < 2; channel++) {
            if (marks[channel] == 0) continue;
            if (now - marks[channel] < cooldownTicks) {
                active = true;
                continue;
            }
            marks[channel] = 0;
            if (health != null) removeModifier(health, MODIFIER_IDS[channel]);
        }
        if (!active) DATA.remove(id);
    }

    public static void clean(LivingEntity entity) {
        long[] marks = DATA.remove(entity.getUUID());
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        for (int channel = 0; channel < MODIFIER_IDS.length; channel++) {
            if (marks == null || marks[channel] != 0) removeModifier(health, MODIFIER_IDS[channel]);
        }
    }

    public static void clean(UUID id) {
        DATA.remove(id);
    }

    private static void removeModifier(AttributeInstance instance, UUID id) {
        if (instance.getModifier(id) != null) instance.removeModifier(id);
    }
}
