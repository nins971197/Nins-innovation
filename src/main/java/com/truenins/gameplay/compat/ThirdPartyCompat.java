package com.truenins.gameplay.compat;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional integrations with mods that are not declared dependencies.
 *
 * <p>Every lookup is guarded: a missing class or member disables the hook once
 * and is never retried, and no failure ever escapes into gameplay code or class
 * initialisation.</p>
 */
public final class ThirdPartyCompat {

    private static final String INVULNERABLE_DATA_CLASS = "com.mega.uom.util.entity.PlayerInvulnerableEntityData";
    private static final String ASM_UTIL_CLASS = "com.mega.uom.util.entity.EntityASMUtil";

    private static boolean invulnerableResolved;
    private static Method invulnerableSetter;

    private static boolean healthDeltaResolved;
    private static EntityDataAccessor<Float> healthDeltaAccessor;

    private ThirdPartyCompat() {}

    public static void clearPlayerInvulnerability(Player player) {
        Method setter = invulnerableSetter();
        if (setter == null) return;
        try {
            setter.invoke(null, player, Boolean.FALSE);
        } catch (Throwable ignored) {
        }
    }

    public static void resetFeHealthDelta(LivingEntity entity) {
        EntityDataAccessor<Float> accessor = healthDeltaAccessor();
        if (accessor == null) return;
        try {
            entity.getEntityData().set(accessor, 0.0F);
        } catch (Throwable ignored) {
        }
    }

    private static synchronized Method invulnerableSetter() {
        if (!invulnerableResolved) {
            invulnerableResolved = true;
            try {
                invulnerableSetter = Class.forName(INVULNERABLE_DATA_CLASS)
                    .getMethod("setInvul", Player.class, boolean.class);
            } catch (Throwable ignored) {
                invulnerableSetter = null;
            }
        }
        return invulnerableSetter;
    }

    @SuppressWarnings("unchecked")
    private static synchronized EntityDataAccessor<Float> healthDeltaAccessor() {
        if (!healthDeltaResolved) {
            healthDeltaResolved = true;
            try {
                Field field = Class.forName(ASM_UTIL_CLASS).getField("FE_GET_HEALTH_DATA");
                healthDeltaAccessor = (EntityDataAccessor<Float>) field.get(null);
            } catch (Throwable ignored) {
                healthDeltaAccessor = null;
            }
        }
        return healthDeltaAccessor;
    }
}
