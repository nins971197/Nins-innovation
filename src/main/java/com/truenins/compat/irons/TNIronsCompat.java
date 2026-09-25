package com.truenins.compat.irons;

import com.truenins.TrueNinsMod;
import com.truenins.spell.CurseZoneHandler;
import com.truenins.spell.InfinitySpellEffect;
import com.truenins.spell.OrbDamage;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.ISSDamageTypes;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.UUID;

public final class TNIronsCompat {

    public static final ResourceLocation CURSE_ID = new ResourceLocation(TrueNinsMod.MODID, "curse");

    public static final TagKey<Item> CURSE_FOCUS =
        TagKey.create(Registries.ITEM, new ResourceLocation(TrueNinsMod.MODID, "curse_focus"));

    public static final DeferredRegister<SchoolType> SCHOOLS =
        DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, TrueNinsMod.MODID);

    public static final DeferredRegister<AbstractSpell> SPELLS =
        DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, TrueNinsMod.MODID);

    public static final RegistryObject<SchoolType> CURSE = SCHOOLS.register("curse", () -> new SchoolType(
        CURSE_ID,
        CURSE_FOCUS,
        Component.translatable("school." + TrueNinsMod.MODID + ".curse")
            .withStyle(style -> style.withColor(0x9BD7FF)),
        AttributeRegistry.SPELL_POWER,
        AttributeRegistry.SPELL_RESIST,
        SoundRegistry.HEARTSTOP_CAST,
        ISSDamageTypes.ELDRITCH_MAGIC));

    public static final RegistryObject<AbstractSpell> INFINITY =
        SPELLS.register("infinity", InfinitySpell::new);

    public static final RegistryObject<AbstractSpell> MADE_IN_HEAVEN =
        SPELLS.register("made_in_heaven", MadeInHeavenSpell::new);

    public static final RegistryObject<AbstractSpell> CURSE_ZONE =
        SPELLS.register("curse_zone", CurseZoneSpell::new);

    public static final RegistryObject<AbstractSpell> AO =
        SPELLS.register("ao", AoSpell::new);

    public static final RegistryObject<AbstractSpell> HE =
        SPELLS.register("he", HeSpell::new);

    private TNIronsCompat() {}

    private static final UUID MANA_LOCK_ID = UUID.fromString("8f2c1d64-3b7a-4e51-9c08-6ad4f1b27e30");

    private static void setManaLock(ServerPlayer player, boolean locked) {
        AttributeInstance attr = player.getAttribute(AttributeRegistry.MANA_REGEN.get());
        if (attr == null) return;
        if (locked) {
            if (attr.getModifier(MANA_LOCK_ID) == null) {
                attr.addTransientModifier(new AttributeModifier(MANA_LOCK_ID,
                    "truenins_infinity_mana_lock", -100.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        } else {
            attr.removeModifier(MANA_LOCK_ID);
        }
    }

    public static void init(IEventBus modBus) {
        SCHOOLS.register(modBus);
        SPELLS.register(modBus);
        InfinitySpellEffect.setManaLock(TNIronsCompat::setManaLock);
        CurseZoneHandler.setManaRefund(TNIronsCompat::refundMana);
        OrbDamage.setHook(TNIronsCompat::spellDamage);
        TrueNinsMod.LOGGER.info("TrueNins: Iron's Spells compat loaded (school truenins:curse)");
    }

    private static boolean spellDamage(LivingEntity target, Entity direct, LivingEntity owner,
                                       float amount, String spellId) {
        if (owner == null) return false;
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        if (spell == null) return false;
        return target.hurt(SpellDamageSource.source(direct, owner, spell), amount);
    }

    private static void refundMana(ServerPlayer player, int amount) {
        MagicData data = MagicData.getPlayerMagicData(player);
        data.addMana(amount);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(data));
    }
}
