package com.truenins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class ReviveHandler {

    private static final Map<UUID, Long> COOLDOWNS_TAG = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS_ENCH = new HashMap<>();

    private static Map<UUID, Long> cooldowns(int channel) {
        return channel == 1 ? COOLDOWNS_ENCH : COOLDOWNS_TAG;
    }

    private ReviveHandler() {}

    public static boolean hasRevive(Player player) {
        if (player.getTags().contains("revive")) return true;
        if (player.getPersistentData().getBoolean("revive")) return true;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack s = player.getItemBySlot(slot);
            if (s.hasTag() && s.getTag().getBoolean("revive")) return true;
            if (TrueNinsEnchantments.levelOf(s, "revive") > 0) return true;
        }
        return false;
    }

    public static boolean isOnCooldown(Player player, long now, int channel) {
        Long last = cooldowns(channel).get(player.getUUID());
        if (last == null) return false;
        long cd = TrueNinsConfig.REVIVE_COOLDOWN_SECONDS.get() * 20L;
        return now - last < cd;
    }

    public static void setCooldown(Player player, long tick, int channel) {
        cooldowns(channel).put(player.getUUID(), tick);
    }

    public static void clean(UUID uuid) {
        COOLDOWNS_TAG.remove(uuid);
        COOLDOWNS_ENCH.remove(uuid);
    }

    public static void execute(Player player) {
        execute(player, 0);
    }

    public static void execute(Player player, int enchantLevel) {

        ArrayList<MobEffectInstance> effects = new ArrayList<>(player.getActiveEffects());
        for (MobEffectInstance inst : effects) {
            if (!inst.getEffect().isBeneficial()) {
                player.removeEffect(inst.getEffect());
            }
        }

        player.deathTime = 0;
        player.hurtTime = 0;

        player.setHealth(player.getMaxHealth());

        if (enchantLevel > 0) {

            int amplifier = Math.min(enchantLevel, 255) - 1;
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, amplifier));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, amplifier));
        } else {

            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 3));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 2));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
        }

        player.level().broadcastEntityEvent(player, (byte) 35);
    }
}
