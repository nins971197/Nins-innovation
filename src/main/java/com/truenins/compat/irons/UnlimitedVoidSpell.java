package com.truenins.compat.irons;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsMod;
import com.truenins.entity.domain.DomainRegistry;
import com.truenins.entity.domain.VoidDomainEntity;
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

public class UnlimitedVoidSpell extends AbstractSpell {

    public static final int CAST_TICKS = 2;

    private static final AnimationHolder CHARGE = new AnimationHolder(
        new ResourceLocation(TrueNinsMod.MODID, "ao_charge"), false, false);
    private static final AnimationHolder FINISH = new AnimationHolder(
        new ResourceLocation(TrueNinsMod.MODID, "ao_throw"), true, false);

    private final DefaultConfig defaultConfig = new DefaultConfig()
        .setMinRarity(SpellRarity.LEGENDARY)
        .setSchoolResource(TNIronsCompat.CURSE_ID)
        .setMaxLevel(1)
        .setCooldownSeconds(120.0D)
        .setAllowCrafting(false)
        .build();

    public UnlimitedVoidSpell() {
        this.castTime = CAST_TICKS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return new ResourceLocation(TrueNinsMod.MODID, "unlimited_void");
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
        return TrueNinsConfig.voidManaCost();
    }

    @Override
    public int getCastTime(int spellLevel) {
        return CAST_TICKS;
    }

    @Override
    public int getSpellCooldown() {
        return TrueNinsConfig.voidCooldownSeconds() * 20;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return CHARGE;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return FINISH;
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

        VoidDomainEntity domain = new VoidDomainEntity(serverLevel, entity,
            TrueNinsConfig.voidDurationTicks(), TrueNinsConfig.voidRadius());
        serverLevel.addFreshEntity(domain);
        // Registers immediately so the caster's previous domain is closed at cast time, not on first tick.
        DomainRegistry.register(domain);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
            Component.translatable("ui.truenins.unlimited_void.mana", TrueNinsConfig.voidManaCost()),
            Component.translatable("ui.truenins.unlimited_void.duration", TrueNinsConfig.voidDurationTicks() / 20),
            Component.translatable("ui.truenins.unlimited_void.radius", (int) TrueNinsConfig.voidRadius()),
            Component.translatable("ui.truenins.unlimited_void.control"));
    }
}
