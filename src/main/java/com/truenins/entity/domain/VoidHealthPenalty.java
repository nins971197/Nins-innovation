package com.truenins.entity.domain;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * The Muryokusho -10% maximum health cut.
 *
 * <p>Players: the cut is recorded in persistent ForgeData, so it survives relog, dimension change and
 * a server restart, while dying clears it again. Before this class the modifier was simply dropped on
 * a living entity and never recorded anywhere, so nothing could re-apply or clear it deliberately.
 *
 * <p>Mobs keep the old session-only behaviour: the modifier applies while the world keeps the entity,
 * and a dead mob never inherits it because the modifier disappears with the entity.
 */
public final class VoidHealthPenalty {

    /** Persistent ForgeData key. Value is the cut as a fraction, 0.10 meaning -10% maximum health. */
    public static final String NBT_KEY = "truenins.void_hp_loss";

    public static final double DEFAULT_LOSS = 0.10D;

    /**
     * Fixed UUID shared with the old implementation. Using the same UUID keeps the modifier unique per
     * entity, so re-applying on login never stacks a second cut on top of the saved one.
     */
    public static final UUID MODIFIER_ID = UUID.fromString("9f8e7d6c-5b4a-3210-9abc-def012345678");

    private static final String MODIFIER_NAME = "truenins.void_health";

    private VoidHealthPenalty() {}

    /** Domain hit: players persist the cut, other living entities only get the session modifier. */
    public static void apply(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            CompoundTag data = player.getPersistentData();
            double loss = data.contains(NBT_KEY) ? data.getDouble(NBT_KEY) : DEFAULT_LOSS;
            if (loss <= 0.0D) loss = DEFAULT_LOSS;
            data.putDouble(NBT_KEY, loss);
            attach(player, loss);
            return;
        }
        // Mobs must not carry the cut into a saved chunk: a transient modifier is never written to
        // entity NBT, so the cut ends with the session exactly as documented instead of becoming a
        // permanent -10% maximum health on every monster that ever crossed a void domain.
        attachTransient(target, DEFAULT_LOSS);
    }

    /** Login, dimension change and entity load: re-attach from the saved record. */
    public static void reattach(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_KEY)) return;
        double loss = data.getDouble(NBT_KEY);
        if (loss <= 0.0D) return;
        attach(player, loss);
    }

    /** Death reset: the cut ends with the life that earned it. */
    public static void clearOnDeath(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) attribute.removeModifier(MODIFIER_ID);
        entity.getPersistentData().remove(NBT_KEY);
    }

    /** Used by the death handler to avoid touching entities that never met the domain. */
    public static boolean hasPenalty(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null && attribute.getModifier(MODIFIER_ID) != null) return true;
        return entity.getPersistentData().contains(NBT_KEY);
    }

    private static void attach(LivingEntity target, double loss) {
        AttributeInstance attribute = target.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        // Idempotent by UUID: the same modifier is never attached twice.
        if (attribute.getModifier(MODIFIER_ID) != null) return;
        attribute.addPermanentModifier(new AttributeModifier(MODIFIER_ID, MODIFIER_NAME, -loss,
            AttributeModifier.Operation.MULTIPLY_TOTAL));
        clampHealth(target);
    }

    private static void attachTransient(LivingEntity target, double loss) {
        AttributeInstance attribute = target.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        if (attribute.getModifier(MODIFIER_ID) != null) return;
        attribute.addTransientModifier(new AttributeModifier(MODIFIER_ID, MODIFIER_NAME, -loss,
            AttributeModifier.Operation.MULTIPLY_TOTAL));
        clampHealth(target);
    }

    private static void clampHealth(LivingEntity target) {
        if (target.getHealth() > target.getMaxHealth()) target.setHealth(target.getMaxHealth());
    }
}
