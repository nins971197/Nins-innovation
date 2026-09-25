package com.truenins.register;

import com.truenins.TrueNinsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TNSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TrueNinsMod.MODID);

    public static final RegistryObject<SoundEvent> BLACK_FLASH =
        SOUNDS.register("black_flash", () -> SoundEvent.createVariableRangeEvent(
            new ResourceLocation(TrueNinsMod.MODID, "black_flash")));

    public static final RegistryObject<SoundEvent> MADE_IN_HEAVEN =
        SOUNDS.register("made_in_heaven", () -> SoundEvent.createVariableRangeEvent(
            new ResourceLocation(TrueNinsMod.MODID, "made_in_heaven")));

    public static final RegistryObject<SoundEvent> AO_CAST =
        SOUNDS.register("ao_cast", () -> SoundEvent.createVariableRangeEvent(
            new ResourceLocation(TrueNinsMod.MODID, "ao_cast")));

    public static final RegistryObject<SoundEvent> AO_HUM =
        SOUNDS.register("ao_hum", () -> SoundEvent.createVariableRangeEvent(
            new ResourceLocation(TrueNinsMod.MODID, "ao_hum")));

    public static final RegistryObject<SoundEvent> HE_CAST =
        SOUNDS.register("he_cast", () -> SoundEvent.createVariableRangeEvent(
            new ResourceLocation(TrueNinsMod.MODID, "he_cast")));

    public static final RegistryObject<SoundEvent> HE_FLY =
        SOUNDS.register("he_fly", () -> SoundEvent.createVariableRangeEvent(
            new ResourceLocation(TrueNinsMod.MODID, "he_fly")));

    public static final RegistryObject<SoundEvent> HE_EXPLODE =
        SOUNDS.register("he_explode", () -> SoundEvent.createFixedRangeEvent(
            new ResourceLocation(TrueNinsMod.MODID, "he_explode"), 128.0F));

    public static final RegistryObject<SoundEvent> BLOCK_BREAK =
        SOUNDS.register("block_break", () -> SoundEvent.createVariableRangeEvent(
            new ResourceLocation(TrueNinsMod.MODID, "block_break")));

    private TNSounds() {}
}
