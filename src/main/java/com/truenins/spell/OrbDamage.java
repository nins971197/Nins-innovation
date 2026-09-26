package com.truenins.spell;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

public final class OrbDamage {

    public interface Hook {
        boolean hurt(LivingEntity target, Entity direct, @Nullable LivingEntity owner, float amount, String spellId);
    }

    private static Hook hook;

    public static void setHook(Hook value) {
        hook = value;
    }

    private OrbDamage() {}

    public static void apply(LivingEntity target, Entity direct, @Nullable LivingEntity owner,
                             float amount, String spellId) {
        if (amount <= 0.0F || !target.isAlive()) return;
        if (owner != null && target == owner) return;
        if (hook != null && hook.hurt(target, direct, owner, amount, spellId)) return;
        target.hurt(target.damageSources().indirectMagic(direct, owner), amount);
    }
}
