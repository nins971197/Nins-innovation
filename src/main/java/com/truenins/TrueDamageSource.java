package com.truenins;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public class TrueDamageSource extends DamageSource {

    public static final ResourceKey<DamageType> KEY =
        ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(TrueNinsMod.MODID, "true_damage"));

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
        String path = tag.location().getPath();
        if (path.startsWith("bypasses_") || path.equals("always_hurts_ender_dragons")) {
            return true;
        }
        return super.is(tag);
    }
}
