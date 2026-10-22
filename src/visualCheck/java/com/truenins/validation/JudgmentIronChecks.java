package com.truenins.validation;

import com.truenins.TrueNinsMod;
import com.truenins.compat.irons.TNIronsCompat;
import com.truenins.entity.domain.JudgmentDomainEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Loaded only when the optional Iron runtime is present in the development client. */
public final class JudgmentIronChecks {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void start(ServerPlayer player,LivingEntity target) {
        var spell=TNIronsCompat.DEADLY_SENTENCING.get();
        check(SpellRegistry.getSpell("truenins:deadly_sentencing")==spell,"spell not registered");
        check(spell.getSchoolType()==TNIronsCompat.CURSE.get()&&spell.getManaCost(1)==200,"school/mana");
        var data=MagicData.getPlayerMagicData(player);
        player.getAttribute(AttributeRegistry.MAX_MANA.get()).setBaseValue(1000);
        player.getAttribute(AttributeRegistry.MANA_REGEN.get()).setBaseValue(0);
        data.resetCastingState();data.getPlayerCooldowns().removeCooldown(spell.getSpellId());
        data.setMana(1000);
        check(spell.checkPreCastConditions(player.level(),1,player,data),"valid target rejected");
        Vec3 original=target.position();target.setPos(original.add(0,0,100));
        spell.castSpell(player.level(),1,player,CastSource.SPELLBOOK,true);
        check(data.getMana()==1000,"invalid completed target spent mana");
        check(player.level().getEntitiesOfClass(JudgmentDomainEntity.class,player.getBoundingBox().inflate(36)).isEmpty(),"invalid target made court");
        target.setPos(original);data.resetCastingState();
        check(spell.attemptInitiateCast(ItemStack.EMPTY,1,player.level(),player,CastSource.SPELLBOOK,true,"mainhand"),"Iron initiation failed");
    }
    public static JudgmentDomainEntity completed(ServerPlayer player,LivingEntity target) {
        var domains=player.level().getEntitiesOfClass(JudgmentDomainEntity.class,player.getBoundingBox().inflate(36));
        check(domains.size()==1,"natural Iron cast did not create exactly one courtroom");
        var court=domains.get(0);var data=MagicData.getPlayerMagicData(player);
        check(court.defendant()==target,"locked defendant changed");
        check(Math.abs(data.getMana()-800)<.01,"natural cast mana: "+data.getMana());
        var event=new io.redspace.ironsspellbooks.api.events.SpellPreCastEvent(player,"truenins:he",1,TNIronsCompat.CURSE.get(),CastSource.SPELLBOOK);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        check(event.isCanceled(),"trial allowed another spell");
        TrueNinsMod.LOGGER.info("JUDGMENT_IRON_CHECK: registry, curse school, failed-target no-cost, natural 2-tick cast, mana=800/1000, locked defendant and spell prevention passed");
        return court;
    }
}
