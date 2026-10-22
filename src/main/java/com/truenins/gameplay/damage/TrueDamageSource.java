package com.truenins.gameplay.damage;

import com.truenins.TrueNinsMod;
import com.truenins.gameplay.tag.TrueNinsContext;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/**
 * True damage source: ignores armour, enchantments, resistance and absorption.
 *
 * <p>The bypass tags are only reported while the true damage window is open, so an
 * instance that escapes the window (for example a death message query later on)
 * behaves like an ordinary custom damage type.</p>
 */
public class TrueDamageSource extends DamageSource {

    private static final ResourceLocation TRUE_DAMAGE_ID = new ResourceLocation(TrueNinsMod.MODID, "true_damage");

    public static final ResourceKey<DamageType> KEY =
        ResourceKey.create(Registries.DAMAGE_TYPE, TRUE_DAMAGE_ID);

    public static TrueDamageSource of(DamageSource original, LivingEntity target) {
        Holder<DamageType> type = target.level().registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(KEY);
        return new TrueDamageSource(type, original);
    }

    private TrueDamageSource(Holder<DamageType> type, DamageSource original) {
        super(type, original.getDirectEntity(), original.getEntity());
    }

    @Override
    public boolean is(TagKey<DamageType> tag) {
        if (TrueNinsContext.inTrueDamage()) {
            String path = tag.location().getPath();
            if (path.startsWith("bypasses_") || path.equals("always_hurts_ender_dragons")) return true;
        }
        return super.is(tag);
    }
}
