package com.truenins.compat.irons;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsMod;
import com.truenins.entity.RedOrbEntity;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class HeSpell extends AbstractSpell {

    public static final int CAST_TICKS = 20;

    private static final AnimationHolder CHARGE = new AnimationHolder(
        new ResourceLocation(TrueNinsMod.MODID, "he_charge"), false, false);
    private static final AnimationHolder THROW = new AnimationHolder(
        new ResourceLocation(TrueNinsMod.MODID, "he_throw"), true, false);

    private final DefaultConfig defaultConfig = new DefaultConfig()
        .setMinRarity(SpellRarity.LEGENDARY)
        .setSchoolResource(TNIronsCompat.CURSE_ID)
        .setMaxLevel(1)
        .setCooldownSeconds(45.0D)
        .setAllowCrafting(false)
        .build();

    public HeSpell() {
        this.castTime = CAST_TICKS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return new ResourceLocation(TrueNinsMod.MODID, "he");
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
        return TrueNinsConfig.heManaCost();
    }

    @Override
    public int getCastTime(int spellLevel) {
        return CAST_TICKS;
    }

    @Override
    public int getSpellCooldown() {
        return TrueNinsConfig.heCooldownSeconds() * 20;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return CHARGE;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return THROW;
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
        return SpellRarity.LEGENDARY;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData magicData) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            com.truenins.register.TNSounds.HE_CAST.get(),
            net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.0F);
        RedOrbEntity orb = new RedOrbEntity(serverLevel, entity);
        serverLevel.addFreshEntity(orb);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
            Component.translatable("ui.truenins.he.mana", TrueNinsConfig.heManaCost()),
            Component.translatable("ui.truenins.he.damage", (int) TrueNinsConfig.heDamageAmount()),
            Component.translatable("ui.truenins.he.range", (int) TrueNinsConfig.heMaxDistance()),
            Component.translatable("ui.truenins.he.tunnel", TrueNinsConfig.heBreakRadius() * 2 + 1));
    }
}
