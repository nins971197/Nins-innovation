package com.truenins.spell;

import com.truenins.TrueNinsConfig;
import com.truenins.register.TNMobEffects;
import com.truenins.register.TNParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class CurseZoneHandler {

    private static final String CHARGES_KEY = "tn_zone_charges";
    private static final double RADIUS = 5.0D;
    private static final int SEGMENTS = 16;

    public interface ManaRefund {
        void refund(ServerPlayer player, int amount);
    }

    private static ManaRefund manaRefund;

    public static void setManaRefund(ManaRefund refund) {
        manaRefund = refund;
    }

    private CurseZoneHandler() {}

    public static void open(LivingEntity caster) {
        caster.getPersistentData().putInt(CHARGES_KEY, TrueNinsConfig.curseZoneMaxTriggers());
        MobEffectInstance current = caster.getEffect(TNMobEffects.CURSE_ZONE.get());
        if (current != null) caster.removeEffect(TNMobEffects.CURSE_ZONE.get());
        caster.addEffect(new MobEffectInstance(
            TNMobEffects.CURSE_ZONE.get(), MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
    }

    public static int charges(LivingEntity caster) {
        CompoundTag data = caster.getPersistentData();
        return data.contains(CHARGES_KEY) ? data.getInt(CHARGES_KEY) : 0;
    }

    public static boolean armed(LivingEntity caster) {
        return caster.hasEffect(TNMobEffects.CURSE_ZONE.get()) && charges(caster) > 0;
    }

    public static void spend(LivingEntity caster) {
        int left = charges(caster) - 1;
        caster.getPersistentData().putInt(CHARGES_KEY, Math.max(0, left));
        if (left <= 0) caster.removeEffect(TNMobEffects.CURSE_ZONE.get());
    }

    public static void refund(LivingEntity caster) {
        if (manaRefund == null) return;
        if (!(caster instanceof ServerPlayer player)) return;
        int amount = TrueNinsConfig.curseZoneManaRefund();
        if (amount <= 0) return;
        manaRefund.refund(player, amount);
    }

    public static void tickFx(LivingEntity entity, long now) {
        if (!entity.hasEffect(TNMobEffects.CURSE_ZONE.get())) return;
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (now % 3L != 0L) return;

        double base = entity.getY() + 0.10D;
        double spin = (now % 240L) * 0.02617993877991494D;

        for (int i = 0; i < SEGMENTS; i++) {
            double angle = spin + i * 6.283185307179586D / SEGMENTS;
            double x = entity.getX() + Math.cos(angle) * RADIUS;
            double z = entity.getZ() + Math.sin(angle) * RADIUS;
            level.sendParticles(TNParticles.MOTE_VEIL.get(), x, base + (i % 4) * 0.55D, z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        if (now % 15L == 0L) {
            level.sendParticles(TNParticles.MOTE_STAR.get(),
                entity.getX(), entity.getY() + 2.35D, entity.getZ(),
                4, 1.5D, 0.45D, 1.5D, 0.0D);
        }
    }
}
