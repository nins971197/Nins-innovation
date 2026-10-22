package com.truenins.gameplay.tag;

import com.truenins.gameplay.enchantment.TrueNinsEnchantments;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Damage context for the true damage channel.
 *
 * <p>The context is derived from the damage source on demand, so no state has to
 * survive the {@code LivingEntity.hurt} call. The only thread local state is a
 * depth counter that is entered and left in the same method (true damage
 * application) plus a server tick reset as a backstop; nothing can leak into
 * unrelated damage on the same thread.</p>
 */
public final class TrueNinsContext {

    public static final class Context {

        public final boolean active;

        public final float originalAmount;

        @Nullable
        public final Float overrideAmount;

        Context(boolean active, float originalAmount, @Nullable Float overrideAmount) {
            this.active = active;
            this.originalAmount = originalAmount;
            this.overrideAmount = overrideAmount;
        }

        public float effectiveAmount() {
            return overrideAmount != null ? overrideAmount : originalAmount;
        }
    }

    private static final Context INACTIVE = new Context(false, 0.0F, null);

    private static final int TRUE_DAMAGE = 0;
    private static final int REFLECT = 1;

    private static final ThreadLocal<int[]> DEPTHS = ThreadLocal.withInitial(() -> new int[2]);

    /**
     * Damage amount the damage pipeline reported for the nested true damage hit of the current
     * attack, or -1 while nothing has landed. Written by the LivingDamageEvent hook, read once by
     * the attacker to decide whether the hit needs to be topped up.
     */
    private static final ThreadLocal<float[]> LANDED = ThreadLocal.withInitial(() -> new float[] {-1.0F});

    private TrueNinsContext() {}

    /** Builds the true damage context for a single hurt call. Never retains state. */
    public static Context of(DamageSource source, float amount) {
        if (source == null) return INACTIVE;
        if (!(source.getEntity() instanceof LivingEntity attacker)) return INACTIVE;
        // Only a direct melee hit counts. A projectile fired while holding a true damage
        // weapon, thorns retaliation or any other reflected damage keeps its own damage
        // type instead of inheriting the weapon's true damage.
        if (source.getDirectEntity() != attacker) return INACTIVE;
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty()) return INACTIVE;
        Float override = readTrueMeAmount(weapon);
        if (override == null) return INACTIVE;
        return new Context(true, amount, override >= 0.0F ? override : null);
    }

    public static boolean inTrueDamage() {
        return DEPTHS.get()[TRUE_DAMAGE] > 0;
    }

    public static void enterTrueDamage() {
        int[] depths = DEPTHS.get();
        if (depths[TRUE_DAMAGE] == 0) LANDED.get()[0] = -1.0F;
        depths[TRUE_DAMAGE]++;
    }

    /** Records a damage amount that really went through the pipeline during the window. */
    public static void recordLanded(float amount) {
        float[] box = LANDED.get();
        if (amount > box[0]) box[0] = amount;
    }

    /** @return the landed amount of the current attack, or a negative value when nothing landed. */
    public static float landed() {
        return LANDED.get()[0];
    }

    public static void exitTrueDamage() {
        int[] depths = DEPTHS.get();
        if (depths[TRUE_DAMAGE] > 0) depths[TRUE_DAMAGE]--;
    }

    public static boolean inReflect() {
        return DEPTHS.get()[REFLECT] > 0;
    }

    public static void enterReflect() {
        DEPTHS.get()[REFLECT]++;
    }

    public static void exitReflect() {
        int[] depths = DEPTHS.get();
        if (depths[REFLECT] > 0) depths[REFLECT]--;
    }

    /** Server tick backstop: clears any depth a crashed or cancelled call may have left behind. */
    public static void reset() {
        int[] depths = DEPTHS.get();
        depths[TRUE_DAMAGE] = 0;
        depths[REFLECT] = 0;
        LANDED.get()[0] = -1.0F;
    }

    /**
     * True damage amount stored on a stack.
     *
     * @return {@code null} when the stack is not a true damage weapon, a negative
     *         value when the weapon deals its own damage as true damage, otherwise
     *         the fixed true damage amount.
     */
    @Nullable
    public static Float readTrueMeAmount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("true_me")) {
            if (tag.contains("TrueDamageAmount", Tag.TAG_FLOAT)) return tag.getFloat("TrueDamageAmount");
            if (tag.contains("TrueDamageAmount", Tag.TAG_INT)) return (float) tag.getInt("TrueDamageAmount");
            Tag value = tag.get("true_me");
            if (value == null) return null;
            return switch (value.getId()) {
                case Tag.TAG_BYTE -> -1.0F;
                case Tag.TAG_INT -> (float) tag.getInt("true_me");
                case Tag.TAG_FLOAT -> tag.getFloat("true_me");
                case Tag.TAG_DOUBLE -> (float) tag.getDouble("true_me");
                default -> -1.0F;
            };
        }
        if (TrueNinsEnchantments.levelOf(stack, "true_me") > 0) return -1.0F;
        return null;
    }
}
