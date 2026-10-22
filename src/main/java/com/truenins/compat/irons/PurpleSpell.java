package com.truenins.compat.irons;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsMod;
import com.truenins.entity.orb.PurpleOrbEntity;
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

/** Cursed technique: Hollow Purple, stationary fusion and detonation. */
public final class PurpleSpell extends AbstractSpell {
    private static final AnimationHolder CHARGE = new AnimationHolder(new ResourceLocation(TrueNinsMod.MODID, "purple_charge"), false, false);
    private static final AnimationHolder FINISH = new AnimationHolder(new ResourceLocation(TrueNinsMod.MODID, "purple_finish"), true, false);
    private final DefaultConfig defaultConfig = new DefaultConfig().setMinRarity(SpellRarity.LEGENDARY)
        .setSchoolResource(TNIronsCompat.CURSE_ID).setMaxLevel(1).setCooldownSeconds(90).setAllowCrafting(false).build();

    public PurpleSpell() { castTime = TrueNinsConfig.purpleFusionTicks(); }
    @Override public ResourceLocation getSpellResource() { return new ResourceLocation(TrueNinsMod.MODID, "purple"); }
    @Override public DefaultConfig getDefaultConfig() { return defaultConfig; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public SchoolType getSchoolType() { return TNIronsCompat.CURSE.get(); }
    @Override public int getManaCost(int spellLevel) { return TrueNinsConfig.purpleManaCost(); }
    @Override public int getCastTime(int spellLevel) { return TrueNinsConfig.purpleFusionTicks(); }
    @Override public int getSpellCooldown() { return TrueNinsConfig.purpleCooldownSeconds() * 20; }
    @Override public AnimationHolder getCastStartAnimation() { return CHARGE; }
    @Override public AnimationHolder getCastFinishAnimation() { return FINISH; }
    @Override public int getMinLevel() { return 1; }
    @Override public int getMaxLevel() { return 1; }
    @Override public SpellRarity getRarity(int spellLevel) { return SpellRarity.LEGENDARY; }

    @Override public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource source, MagicData data) {
        if (!level.isClientSide && level instanceof ServerLevel server) server.addFreshEntity(new PurpleOrbEntity(server, entity));
    }

    @Override public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.truenins.purple.mana", TrueNinsConfig.purpleManaCost()),
            Component.translatable("ui.truenins.purple.damage", (int)TrueNinsConfig.purpleDamageAmount()),
            Component.translatable("ui.truenins.purple.radius", (int)TrueNinsConfig.purpleBlastRadius()),
            Component.translatable("ui.truenins.purple.terrain", TrueNinsConfig.purpleBreakBlocks()));
    }
}
