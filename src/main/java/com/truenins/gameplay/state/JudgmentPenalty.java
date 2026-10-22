package com.truenins.gameplay.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/**
 * The light verdict of Deadly Sentencing ("Confiscation" / 轻罪 · 剥夺).
 *
 * <p>The defendant loses 30% of every hit it deals for 30 seconds. The sentence lives in
 * the defendant's own persistent data as a tick countdown, so it survives a relog, a
 * chunk reload or a dimension change, and the damage cut is applied in one place:
 * {@code TagGameplayEvents#onConfiscationHurt}, which scales the hit the defendant caused.</p>
 *
 * <p>Deliberately not an attribute modifier: attack damage is only one way an entity can
 * deal damage (arrows, spells and thrown potions all skip it) and a permanent-looking
 * modifier on a mob would be another piece of state that can be stranded by other mods.</p>
 */
public final class JudgmentPenalty {

    /** 30 seconds, the same window the old Weakness III + Slowness III pair used. */
    public static final int DURATION_TICKS = 600;
    /** Multiplier applied to every hit the defendant causes. */
    public static final float DAMAGE_MULTIPLIER = 0.70F;

    private static final String NBT_TICKS = "truenins_confiscation_ticks";

    private JudgmentPenalty() {}

    public static void apply(LivingEntity defendant) {
        apply(defendant, DURATION_TICKS);
    }

    public static void apply(LivingEntity defendant, int durationTicks) {
        if (defendant == null || defendant.level().isClientSide) return;
        defendant.getPersistentData().putInt(NBT_TICKS, Math.max(1, durationTicks));
    }

    /** True while the sentence is still running. */
    public static boolean isSentenced(LivingEntity entity) {
        if (entity == null) return false;
        CompoundTag data = entity.getPersistentData();
        return data.contains(NBT_TICKS) && data.getInt(NBT_TICKS) > 0;
    }

    /** Whole remaining seconds, for the command/guide feedback. */
    public static int secondsLeft(LivingEntity entity) {
        if (entity == null) return 0;
        return Math.max(0, entity.getPersistentData().getInt(NBT_TICKS)) / 20;
    }

    /** Counts the sentence down. Safe to call every tick, from both sides. */
    public static void tick(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_TICKS)) return;
        int left = data.getInt(NBT_TICKS) - 1;
        if (left <= 0) data.remove(NBT_TICKS);
        else data.putInt(NBT_TICKS, left);
    }

    public static void clear(LivingEntity entity) {
        if (entity == null) return;
        entity.getPersistentData().remove(NBT_TICKS);
    }
}
