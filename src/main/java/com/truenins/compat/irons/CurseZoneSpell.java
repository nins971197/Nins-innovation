package com.truenins.compat.irons;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsMod;
import com.truenins.spell.CurseZoneHandler;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class CurseZoneSpell extends AbstractSpell {

    public static final int CAST_TICKS = 20;

    private final DefaultConfig defaultConfig = new DefaultConfig()
        .setMinRarity(SpellRarity.RARE)
        .setSchoolResource(TNIronsCompat.CURSE_ID)
        .setMaxLevel(1)
        .setCooldownSeconds(180.0D)
        .setAllowCrafting(false)
        .build();

    public CurseZoneSpell() {
        this.castTime = CAST_TICKS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return new ResourceLocation(TrueNinsMod.MODID, "curse_zone");
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public SchoolType getSchoolType() {
        return TNIronsCompat.CURSE.get();
    }

    @Override
    public int getManaCost(int spellLevel) {
        return TrueNinsConfig.curseZoneManaCost();
    }

    @Override
    public int getSpellCooldown() {
        return TrueNinsConfig.curseZoneCooldownSeconds() * 20;
    }

    @Override
    public int getCastTime(int spellLevel) {
        return CAST_TICKS;
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public SpellRarity getRarity(int spellLevel) {
        return SpellRarity.RARE;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData magicData) {
        if (level.isClientSide) return;
        CurseZoneHandler.open(entity);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
            Component.translatable("ui.truenins.curse_zone.mana", TrueNinsConfig.curseZoneManaCost()),
            Component.translatable("ui.truenins.curse_zone.charges", TrueNinsConfig.curseZoneMaxTriggers()),
            Component.translatable("ui.truenins.curse_zone.black_flash", TrueNinsConfig.curseZoneBlackFlashChance()),
            Component.translatable("ui.truenins.curse_zone.refund", TrueNinsConfig.curseZoneManaRefund()));
    }
}
