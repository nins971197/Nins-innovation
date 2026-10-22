package com.truenins.compat.irons;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsMod;
import com.truenins.entity.ConflagrationEntity;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.List;

public final class EternalConflagrationSpell extends AbstractSpell {
    private final DefaultConfig config=new DefaultConfig().setMinRarity(SpellRarity.LEGENDARY)
        .setSchoolResource(TNIronsCompat.CURSE_ID).setMaxLevel(1).setCooldownSeconds(180).setAllowCrafting(false).build();
    @Override public ResourceLocation getSpellResource(){return new ResourceLocation(TrueNinsMod.MODID,"eternal_conflagration");}
    @Override public DefaultConfig getDefaultConfig(){return config;}
    @Override public CastType getCastType(){return CastType.INSTANT;}
    @Override public SchoolType getSchoolType(){return TNIronsCompat.CURSE.get();}
    @Override public int getManaCost(int level){return TrueNinsConfig.conflagrationManaCost();}
    @Override public int getCastTime(int level){return 0;}
    @Override public int getSpellCooldown(){return TrueNinsConfig.conflagrationCooldownSeconds()*20;}
    @Override public int getMinLevel(){return 1;}
    @Override public int getMaxLevel(){return 1;}
    @Override public SpellRarity getRarity(int level){return SpellRarity.LEGENDARY;}
    @Override public boolean checkPreCastConditions(Level level,int spellLevel,LivingEntity caster,MagicData data) {
        return !(level instanceof ServerLevel server)||ConflagrationEntity.available(server);
    }
    @Override public void onCast(Level level,int spellLevel,LivingEntity caster,CastSource source,MagicData data) {
        if(level instanceof ServerLevel server&&ConflagrationEntity.available(server))server.addFreshEntity(new ConflagrationEntity(server,caster));
    }
    @Override public List<MutableComponent> getUniqueInfo(int level,LivingEntity caster) {
        return List.of(Component.translatable("ui.truenins.conflagration.radius",(int)TrueNinsConfig.conflagrationRadius()),
            Component.translatable("ui.truenins.conflagration.terrain",TrueNinsConfig.conflagrationBreakBlocks()));
    }
}
