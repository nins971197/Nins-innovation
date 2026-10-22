package com.truenins.compat.irons;

import com.truenins.TrueNinsMod;
import com.truenins.entity.domain.DomainLifecycle;
import com.truenins.spell.CurseZoneHandler;
import com.truenins.spell.InfinitySpellEffect;
import com.truenins.spell.OrbDamage;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
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

    public static final RegistryObject<AbstractSpell> PURPLE =
        SPELLS.register("purple", PurpleSpell::new);

    public static final RegistryObject<AbstractSpell> UNLIMITED_VOID =
        SPELLS.register("unlimited_void", UnlimitedVoidSpell::new);

    public static final RegistryObject<AbstractSpell> SHINGAN =
        SPELLS.register("shingan", ShinganSpell::new);

    public static final RegistryObject<AbstractSpell> DEADLY_SENTENCING =
        SPELLS.register("deadly_sentencing", DeadlySentencingSpell::new);

    public static final RegistryObject<AbstractSpell> ETERNAL_CONFLAGRATION =
        SPELLS.register("eternal_conflagration", EternalConflagrationSpell::new);

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
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(TNIronsCompat::denySpellInsideVoid);
        SCHOOLS.register(modBus);
        SPELLS.register(modBus);
        InfinitySpellEffect.setManaLock(TNIronsCompat::setManaLock);
        CurseZoneHandler.setManaRefund(TNIronsCompat::refundMana);
        OrbDamage.setHook(TNIronsCompat::spellDamage);
        OrbDamage.setManaHook(TNIronsCompat::drainMana);
        // Optional Iron integration (P0-3): restore Made in Heaven's world clock when the server stops.
        DomainLifecycle.setOptionalShutdownHook(() -> MadeInHeavenSpell.restoreAll("server-stopping"));
        // Optional integration safety net: a caster that dies, logs out or changes dimension must not
        // leave a permanently accelerated world clock behind if Iron never reports the cast as finished.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) MadeInHeavenSpell.tickSessions(event.getServer());
        });
        // Compiled against Iron 3.16.3, allowed at runtime from 3.15.0. Every API member this mod calls
        // was verified identical in both jars with javap, so no reflection layer is required; the pinned
        // version is logged so a future incompatibility is diagnosable from the log alone.
        TrueNinsMod.LOGGER.info("TrueNins: Iron's Spells {} detected, compat loaded (school truenins:curse)",
            ironVersion());
    }

    private static String ironVersion() {
        return ModList.get().getModContainerById("irons_spellbooks")
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("unknown");
    }

    private static boolean degraded;

    /**
     * Version tolerance: a NoSuchMethodError / NoClassDefFoundError thrown by a different Iron build
     * must cost one feature, not the whole server. Logged once so the log is not spammed.
     */
    private static void degrade(String feature, LinkageError error) {
        if (degraded) return;
        degraded = true;
        TrueNinsMod.LOGGER.warn("TrueNins: Iron compat degraded for '{}' (Iron {}): {}",
            feature, ironVersion(), error.toString());
    }

    @SubscribeEvent
    public static void denySpellInsideVoid(SpellPreCastEvent event) {
        if (com.truenins.spell.domain.VoidControl.isAffected(event.getEntity())
                || com.truenins.spell.domain.JudgmentControl.isAffected(event.getEntity())) event.setCanceled(true);
    }

    private static boolean spellDamage(LivingEntity target, Entity direct, LivingEntity owner,
                                       float amount, String spellId) {
        if (owner == null) return false;
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        if (spell == null) return false;
        try {
            target.hurt(SpellDamageSource.source(direct, owner, spell), amount);
            return true;
        } catch (LinkageError error) {
            // Falls through to the plain magic damage the caller applies when the hook declines.
            degrade("spell damage", error);
            return false;
        }
    }

    private static void refundMana(ServerPlayer player, int amount) {
        try {
            MagicData data = MagicData.getPlayerMagicData(player);
            data.addMana(amount);
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(data));
        } catch (LinkageError error) {
            degrade("mana refund", error);
        }
    }

    private static boolean drainMana(LivingEntity caster, int amount) {
        if (!(caster instanceof ServerPlayer player)) return false;
        try {
            MagicData data = MagicData.getPlayerMagicData(player);
            if (data.getMana() < amount) {
                io.redspace.ironsspellbooks.api.util.Utils.serverSideCancelCast(player);
                return false;
            }
            data.setMana(data.getMana() - amount);
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(data));
            return true;
        } catch (LinkageError error) {
            // Let the cast continue rather than abort a player's ability over a version difference.
            degrade("mana drain", error);
            return true;
        }
    }
}
