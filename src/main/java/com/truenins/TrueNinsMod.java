package com.truenins;

import com.truenins.gameplay.enchantment.TrueNinsEnchantments;
import com.truenins.register.TNEntities;
import com.truenins.register.TNItems;
import com.truenins.register.TNMobEffects;
import com.truenins.register.TNParticles;
import com.truenins.register.TNSounds;

import com.mojang.logging.LogUtils;
import com.truenins.gameplay.TagGameplayEvents;
import com.truenins.register.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Forge entry point. Content registration and optional integration only. */
@Mod(TrueNinsMod.MODID)
public final class TrueNinsMod {
    public static final String MODID = "truenins";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TrueNinsMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TrueNinsConfig.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.register(new TagGameplayEvents());
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        TNItems.ITEMS.register(bus);
        TNMobEffects.EFFECTS.register(bus);
        TNParticles.PARTICLES.register(bus);
        TNEntities.ENTITIES.register(bus);
        com.truenins.spell.conflagration.ConflagrationNetwork.init();
        TNSounds.SOUNDS.register(bus);
        TrueNinsEnchantments.ENCHANTMENTS.register(bus);
        bus.addListener(TNItems::onBuildCreativeTab);
        if (ModList.get().isLoaded("irons_spellbooks")) {
            com.truenins.compat.irons.TNIronsCompat.init(bus);
        }
        LOGGER.info("TrueNins initialized");
    }

    // Stable entry points used by mixins and integrations.
    public static boolean hasTag(LivingEntity entity, String tag) {
        return TagGameplayEvents.hasTag(entity, tag);
    }
    public static void blackFlashFx(LivingEntity entity, boolean burst) {
        TagGameplayEvents.blackFlashFx(entity, burst);
    }
    public static void clearFeInvulnerability(LivingEntity entity) {
        TagGameplayEvents.clearFeInvulnerability(entity);
    }
    public static void resetFeDelta(LivingEntity entity) {
        TagGameplayEvents.resetFeDelta(entity);
    }
}
