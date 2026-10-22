package com.truenins.gameplay.state;

import com.truenins.TrueNinsConfig;
import com.truenins.gameplay.tag.TrueninsChannels;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Revive brings a player back once and then recharges per channel. A single
 * revive still applies every channel bundle that was off cooldown, so a player
 * carrying both sources gets both reward sets without reviving twice.
 */
public final class ReviveHandler {

    /** Channel cooldown marks: index 0 is the NBT channel, index 1 the enchantment. */
    private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();

    private ReviveHandler() {}

    /** @return true when the player was revived and the death event should be cancelled. */
    public static boolean tryRevive(Player player, long now, int channelMask) {
        long[] marks = COOLDOWNS.computeIfAbsent(player.getUUID(), key -> new long[2]);
        int ready = readyChannels(marks, now, channelMask);
        if (ready == 0) return false;

        revive(player, ready);
        for (int channel = 0; channel < 2; channel++) {
            if ((ready & TrueninsChannels.bit(channel)) != 0) marks[channel] = now;
        }
        return true;
    }

    private static int readyChannels(long[] marks, long now, int channelMask) {
        long cooldownTicks = TrueNinsConfig.REVIVE_COOLDOWN_SECONDS.get() * 20L;
        int ready = 0;
        for (int channel = 0; channel < 2; channel++) {
            int bit = TrueninsChannels.bit(channel);
            if ((channelMask & bit) == 0) continue;
            long mark = marks[channel];
            if (mark != 0L && now - mark < cooldownTicks) continue;
            ready |= bit;
        }
        return ready;
    }

    private static void revive(Player player, int channels) {
        for (MobEffectInstance instance : new ArrayList<>(player.getActiveEffects())) {
            if (!instance.getEffect().isBeneficial()) player.removeEffect(instance.getEffect());
        }

        player.deathTime = 0;
        player.hurtTime = 0;
        player.setHealth(player.getMaxHealth());

        if ((channels & TrueninsChannels.NBT_BIT) != 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 3));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 2));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
        }
        if ((channels & TrueninsChannels.ENCHANT_BIT) != 0) {
            int amplifier = Math.min(TrueninsChannels.level(player, "revive"), 255) - 1;
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, Math.max(0, amplifier)));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, Math.max(0, amplifier)));
        }

        player.level().broadcastEntityEvent(player, (byte) 35);
    }

    public static void clean(UUID id) {
        COOLDOWNS.remove(id);
    }
}
