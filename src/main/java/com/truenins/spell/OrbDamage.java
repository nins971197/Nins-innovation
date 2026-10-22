package com.truenins.spell;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

public final class OrbDamage {

    public interface Hook {
        boolean hurt(LivingEntity target, Entity direct, @Nullable LivingEntity owner, float amount, String spellId);
    }

    public interface ManaHook {
        boolean drain(LivingEntity caster, int amount);
    }

    private static Hook hook;
    private static ManaHook manaHook;

    public static void setHook(Hook value) {
        hook = value;
    }

    public static void setManaHook(ManaHook value) {
        manaHook = value;
    }

    private OrbDamage() {}

    public static boolean drainMana(LivingEntity caster, int amount) {
        return manaHook != null && manaHook.drain(caster, amount);
    }

    public static void apply(LivingEntity target, Entity direct, @Nullable LivingEntity owner,
                             float amount, String spellId) {
        if (amount <= 0.0F || !target.isAlive()) return;
        if (owner != null && target == owner) return;
        if (hook != null && hook.hurt(target, direct, owner, amount, spellId)) return;
        target.hurt(target.damageSources().indirectMagic(direct, owner), amount);
    }
}
