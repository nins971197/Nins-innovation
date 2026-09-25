package com.truenins;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public final class TrueNinsContext {

    private static final ThreadLocal<Deque<Context>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    public static final class Context {

        public boolean active;

        public boolean fromEnchantment;

        public float originalAmount;

        @Nullable
        public Float overrideAmount;

        public float preDamageHealth;

        public float preAbsorption;

        public Context(boolean active, float originalAmount, @Nullable Float overrideAmount) {
            this(active, originalAmount, overrideAmount, false);
        }

        public Context(boolean active, float originalAmount, @Nullable Float overrideAmount,
                       boolean fromEnchantment) {
            this.active = active;
            this.originalAmount = originalAmount;
            this.overrideAmount = overrideAmount;
            this.fromEnchantment = fromEnchantment;
            this.preDamageHealth = -1f;
            this.preAbsorption = 0f;
        }

        public float effectiveAmount() {
            return overrideAmount != null ? overrideAmount : originalAmount;
        }
    }

    private TrueNinsContext() {}

    public static void push(Context ctx) {
        STACK.get().push(ctx);
    }

    @Nullable
    public static Context peek() {
        Deque<Context> stack = STACK.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    public static void pop() {
        Deque<Context> stack = STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static boolean isActive() {
        Context ctx = peek();
        return ctx != null && ctx.active;
    }

    @Nullable
    public static Float readTrueMeAmount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("true_me")) {
            if (tag.contains("TrueDamageAmount", Tag.TAG_FLOAT)) return tag.getFloat("TrueDamageAmount");
            if (tag.contains("TrueDamageAmount", Tag.TAG_INT)) return (float) tag.getInt("TrueDamageAmount");
            Tag value = tag.get("true_me");
            if (value == null) return null;
            return switch (value.getId()) {
                case Tag.TAG_BYTE -> -1f;
                case Tag.TAG_INT -> (float) tag.getInt("true_me");
                case Tag.TAG_FLOAT -> tag.getFloat("true_me");
                case Tag.TAG_DOUBLE -> (float) tag.getDouble("true_me");
                default -> -1f;
            };
        }
        if (TrueNinsEnchantments.levelOf(stack, "true_me") > 0) return -1f;
        return null;
    }

    public static boolean isTrueMeAttack(DamageSource source) {
        if (isActive()) return true;
        if (!(source.getEntity() instanceof LivingEntity attacker)) return false;
        ItemStack weapon = attacker.getMainHandItem();
        return !weapon.isEmpty() && readTrueMeAmount(weapon) != null;
    }
}
