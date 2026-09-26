package com.truenins.spell;

import com.truenins.TrueNinsConfig;
import com.truenins.register.TNMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class InfinitySpellEffect {

    public static int flyTicks() {
        return TrueNinsConfig.infinityDurationTicks();
    }

    public static int drainInterval() {
        return TrueNinsConfig.infinityDrainIntervalTicks();
    }

    public static float drainAmount() {
        return TrueNinsConfig.infinityDrainAmount();
    }

    private static final String KEY_ON = "tn_inf_on";
    private static final String KEY_FLY_END = "tn_inf_fly_end";
    private static final String KEY_DRAIN_NEXT = "tn_inf_drain_next";

    public static final String KEY_FREE = "tn_inf_free";

    public interface ManaLock {
        void set(ServerPlayer player, boolean locked);
    }

    private static ManaLock manaLock;

    public static void setManaLock(ManaLock hook) {
        manaLock = hook;
    }

    private static void applyManaLock(ServerPlayer player, boolean locked) {
        if (manaLock != null) manaLock.set(player, locked);
    }

    private InfinitySpellEffect() {}

    public static void start(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        long now = player.level().getGameTime();
        int duration = flyTicks();
        data.putBoolean(KEY_ON, true);
        data.putBoolean(KEY_FREE, true);
        data.putBoolean("infinity", true);
        data.putBoolean("fly", true);
        data.putLong(KEY_FLY_END, now + duration);
        data.putLong(KEY_DRAIN_NEXT, now + duration + drainInterval());

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, false, true));
        refreshMarker(player);
        applyManaLock(player, true);
    }

    public static boolean isActive(ServerPlayer player) {
        return player.getPersistentData().getBoolean(KEY_ON);
    }

    public static void tick(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(KEY_ON)) return;

        applyManaLock(player, true);

        long now = player.level().getGameTime();
        long flyEnd = data.getLong(KEY_FLY_END);

        if (now < flyEnd) {
            data.putBoolean("infinity", true);
            data.putBoolean("fly", true);
            refreshMarker(player);
            return;
        }

        if (data.getBoolean("fly")) {
            data.remove("fly");
        }
        if (!data.getBoolean("infinity")) {
            stop(player);
            return;
        }

        refreshMarker(player);

        if (player.isCreative() || player.isSpectator()) return;

        if (now < data.getLong(KEY_DRAIN_NEXT)) return;
        data.putLong(KEY_DRAIN_NEXT, now + drainInterval());

        float amount = drainAmount();
        float health = player.getHealth();
        if (health <= amount) {
            stop(player);
            return;
        }
        player.setHealth(health - amount);
    }

    public static void stop(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.putBoolean(KEY_ON, false);
        data.remove(KEY_FREE);
        data.remove("infinity");
        data.remove("fly");
        data.remove(KEY_FLY_END);
        data.remove(KEY_DRAIN_NEXT);
        player.removeEffect(TNMobEffects.INFINITY_FIELD.get());
        applyManaLock(player, false);
    }

    private static void refreshMarker(ServerPlayer player) {
        MobEffectInstance current = player.getEffect(TNMobEffects.INFINITY_FIELD.get());
        if (current == null || current.getDuration() < 100) {
            player.addEffect(new MobEffectInstance(TNMobEffects.INFINITY_FIELD.get(), 400, 0, false, false, true));
        }
    }
}
