package com.truenins.register;

import com.truenins.TrueNinsMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TNMobEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TrueNinsMod.MODID);

    public static final RegistryObject<MobEffect> INFINITY_FIELD =
        EFFECTS.register("infinity_field", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x78BEEB) {});

    public static final RegistryObject<MobEffect> BLACK_FLASH =
        EFFECTS.register("black_flash", () -> new MobEffect(MobEffectCategory.NEUTRAL, 0x1A0A14) {});

    public static final RegistryObject<MobEffect> MADE_IN_HEAVEN =
        EFFECTS.register("made_in_heaven", () -> new MobEffect(MobEffectCategory.NEUTRAL, 0x96DCFF) {});

    public static final RegistryObject<MobEffect> CURSE_ZONE =
        EFFECTS.register("curse_zone", () -> new MobEffect(MobEffectCategory.NEUTRAL, 0x2A1740) {});

    private TNMobEffects() {}
}
