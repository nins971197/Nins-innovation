package com.truenins.compat.irons;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsMod;
import com.truenins.entity.domain.DomainRegistry;
import com.truenins.entity.domain.JudgmentDomainEntity;
import com.truenins.spell.domain.JudgmentTargeting;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.List;

/** Locks one visible defendant before mana is spent; never retargets a completed cast. */
public final class DeadlySentencingSpell extends AbstractSpell {
    private final DefaultConfig config = new DefaultConfig().setMinRarity(SpellRarity.LEGENDARY)
        .setSchoolResource(TNIronsCompat.CURSE_ID).setMaxLevel(1).setCooldownSeconds(120)
        .setAllowCrafting(false).build();
    public DeadlySentencingSpell() { castTime = 2; }
    @Override public ResourceLocation getSpellResource() { return new ResourceLocation(TrueNinsMod.MODID, "deadly_sentencing"); }
    @Override public DefaultConfig getDefaultConfig() { return config; }
    @Override public SchoolType getSchoolType() { return TNIronsCompat.CURSE.get(); }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public int getManaCost(int level) { return TrueNinsConfig.judgmentManaCost(); }
    @Override public int getSpellCooldown() { return TrueNinsConfig.judgmentCooldownSeconds() * 20; }
    @Override public int getCastTime(int level) { return 2; }
    @Override public int getMinLevel() { return 1; }
    @Override public int getMaxLevel() { return 1; }
    @Override public SpellRarity getRarity(int level) { return SpellRarity.LEGENDARY; }
    @Override public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        if (level.isClientSide) return true;
        LivingEntity target = JudgmentTargeting.pick(caster, TrueNinsConfig.judgmentTargetRange());
        if (target == null || !JudgmentTargeting.available(caster, target)) {
            if (caster instanceof Player p) p.displayClientMessage(Component.translatable("ui.truenins.judgment.no_target"), true);
            return false;
        }
        data.setAdditionalCastData(new TargetEntityCastData(target));
        return true;
    }
    @Override public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (!(level instanceof ServerLevel server) || !(data.getAdditionalCastData() instanceof TargetEntityCastData locked)) return;
        LivingEntity target = locked.getTarget(server);
        if (!JudgmentTargeting.valid(caster, target, TrueNinsConfig.judgmentTargetRange())
                || !JudgmentTargeting.available(caster, target)) return;
        JudgmentDomainEntity domain = new JudgmentDomainEntity(server, caster, target);
        server.addFreshEntity(domain);
        // Registers immediately so the caster's previous domain is closed at cast time, not on first tick.
        DomainRegistry.register(domain);
    }
    @Override public void castSpell(Level level, int spellLevel, net.minecraft.server.level.ServerPlayer caster,
                                    CastSource source, boolean triggerCooldown) {
        // Iron spends mana before onCast. Recheck the locked target before that step too.
        MagicData data = MagicData.getPlayerMagicData(caster);
        if (!(level instanceof ServerLevel server)
                || !(data.getAdditionalCastData() instanceof TargetEntityCastData locked)
                || !JudgmentTargeting.valid(caster, locked.getTarget(server), TrueNinsConfig.judgmentTargetRange())
                || !JudgmentTargeting.available(caster, locked.getTarget(server))) {
            caster.displayClientMessage(Component.translatable("ui.truenins.judgment.no_target"), true);
            return;
        }
        super.castSpell(level, spellLevel, caster, source, triggerCooldown);
    }
    @Override public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        return List.of(Component.translatable("ui.truenins.judgment.mana", TrueNinsConfig.judgmentManaCost()),
            Component.translatable("ui.truenins.judgment.range", (int)TrueNinsConfig.judgmentTargetRange()),
            Component.translatable("ui.truenins.judgment.rules"));
    }
}
