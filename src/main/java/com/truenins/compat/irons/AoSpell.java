package com.truenins.compat.irons;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsMod;
import com.truenins.entity.BlueOrbEntity;
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

public class AoSpell extends AbstractSpell {

    public static final int CAST_TICKS = 20;

    private static final AnimationHolder CHARGE = new AnimationHolder(
        new ResourceLocation(TrueNinsMod.MODID, "ao_charge"), false, false);
    private static final AnimationHolder THROW = new AnimationHolder(
        new ResourceLocation(TrueNinsMod.MODID, "ao_throw"), true, false);

    private final DefaultConfig defaultConfig = new DefaultConfig()
        .setMinRarity(SpellRarity.LEGENDARY)
        .setSchoolResource(TNIronsCompat.CURSE_ID)
        .setMaxLevel(1)
        .setCooldownSeconds(30.0D)
        .setAllowCrafting(false)
        .build();

    public AoSpell() {
        this.castTime = CAST_TICKS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return new ResourceLocation(TrueNinsMod.MODID, "ao");
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
        return TrueNinsConfig.aoManaCost();
    }

    @Override
    public int getCastTime(int spellLevel) {
        return CAST_TICKS;
    }

    @Override
    public int getSpellCooldown() {
        return TrueNinsConfig.aoCooldownSeconds() * 20;
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
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, MagicData magicData) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlueOrbEntity orb = new BlueOrbEntity(serverLevel, entity, getCastTime(spellLevel));
        serverLevel.addFreshEntity(orb);
        BlueOrbEntity.CHARGING.put(entity.getUUID(), orb);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData magicData) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            com.truenins.register.TNSounds.AO_CAST.get(),
            net.minecraft.sounds.SoundSource.BLOCKS, 1.5F, 1.0F);

        BlueOrbEntity orb = BlueOrbEntity.CHARGING.remove(entity.getUUID());
        if (orb == null || orb.isRemoved()) {
            orb = new BlueOrbEntity(serverLevel, entity, 1);
            serverLevel.addFreshEntity(orb);
        }
        orb.launch();
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity,
                                     MagicData magicData, boolean cancelled) {
        if (cancelled) {
            BlueOrbEntity orb = BlueOrbEntity.CHARGING.remove(entity.getUUID());
            if (orb != null) orb.discard();
        }
        super.onServerCastComplete(level, spellLevel, entity, magicData, cancelled);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
            Component.translatable("ui.truenins.ao.mana", TrueNinsConfig.aoManaCost()),
            Component.translatable("ui.truenins.ao.duration", TrueNinsConfig.aoDurationTicks() / 20),
            Component.translatable("ui.truenins.ao.radius", (int) TrueNinsConfig.aoMaxDistance()),
            Component.translatable("ui.truenins.ao.damage", (int) TrueNinsConfig.aoDamageAmount()));
    }
}
