package com.truenins;

import com.mojang.logging.LogUtils;
import com.truenins.TrueNinsContext.Context;
import com.truenins.command.TrueNinsCommand;
import com.truenins.effect.BlackFlashFlight;
import com.truenins.register.TNEntities;
import com.truenins.register.TNItems;
import com.truenins.register.TNMobEffects;
import com.truenins.register.TNParticles;
import com.truenins.register.TNSounds;
import com.truenins.spell.CurseZoneHandler;
import com.truenins.spell.InfinitySpellEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.*;

@Mod(TrueNinsMod.MODID)
public class TrueNinsMod {

    public static final String MODID = "truenins";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<Boolean> REENTER = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> THORNS_REENTER = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Boolean> SAVED_MAYFLY = new HashMap<>();
    private static net.minecraft.network.syncher.EntityDataAccessor<Float> feDeltaAccessor;
    private static boolean feDeltaLookupDone;
    private static final Map<UUID, Long> ANTIHEAL_COOLDOWNS = new HashMap<>();

    public TrueNinsMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TrueNinsConfig.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        TNItems.ITEMS.register(modBus);
        TNMobEffects.EFFECTS.register(modBus);
        TNParticles.PARTICLES.register(modBus);
        TNEntities.ENTITIES.register(modBus);
        TNSounds.SOUNDS.register(modBus);
        TrueNinsEnchantments.ENCHANTMENTS.register(modBus);
        modBus.addListener(TNItems::onBuildCreativeTab);

        if (net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) {
            com.truenins.compat.irons.TNIronsCompat.init(modBus);
        }

        LOGGER.info("TrueNins v2.9.0 — enchantments / colorfast / unclear / tag guide");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        TrueNinsCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    public static boolean hasTag(LivingEntity entity, String tag) {

        if (entity.getTags().contains(tag)) return true;

        if (entity.getPersistentData().getBoolean(tag)) return true;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
                ItemStack s = entity.getItemBySlot(slot);
                if (s.hasTag() && s.getTag().getBoolean(tag)) return true;
                if (TrueNinsEnchantments.levelOf(s, tag) > 0) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    @SubscribeEvent
    public void onEquipmentChange(LivingEquipmentChangeEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!e.getSlot().isArmor()) return;

        updateFly(sp);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && hasTag(sp, "fly")) updateFly(sp);
    }
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            if (InfinitySpellEffect.isActive(sp)) InfinitySpellEffect.stop(sp);
            if (hasTag(sp, "fly")) updateFly(sp);
        }
    }

    @SubscribeEvent
    public void onInfinitySpellDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && InfinitySpellEffect.isActive(sp)) {
            InfinitySpellEffect.stop(sp);
        }
    }
    @SubscribeEvent
    public void onPlayerDimChange(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && hasTag(sp, "fly")) updateFly(sp);
    }
    @SubscribeEvent
    public void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && hasTag(sp, "fly")) updateFly(sp);
    }

    private static void updateFly(ServerPlayer sp) {
        if (sp.isCreative() || sp.isSpectator()) return;
        boolean want = hasTag(sp, "fly");
        Boolean saved = SAVED_MAYFLY.get(sp.getUUID());
        if (want) {
            if (saved == null) SAVED_MAYFLY.put(sp.getUUID(), sp.getAbilities().mayfly);
            if (!sp.getAbilities().mayfly) {
                sp.getAbilities().mayfly = true;
                sp.onUpdateAbilities();
            }
            sp.fallDistance = 0f;
        } else if (saved != null) {
            SAVED_MAYFLY.remove(sp.getUUID());
            sp.getAbilities().mayfly = saved;
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
        }
    }

    @SubscribeEvent
    public void onImmunityEffect(MobEffectEvent.Applicable e) {
        net.minecraft.world.effect.MobEffect applied = e.getEffectInstance().getEffect();
        if (applied == TNMobEffects.BLACK_FLASH.get()
            || applied == TNMobEffects.CURSE_ZONE.get()
            || applied == TNMobEffects.MADE_IN_HEAVEN.get()) return;
        if (hasTag(e.getEntity(), "immunity") &&
            applied.getCategory() != net.minecraft.world.effect.MobEffectCategory.BENEFICIAL) {
            e.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlackFlashApplicable(MobEffectEvent.Applicable e) {
        if (e.getEffectInstance().getEffect() == TNMobEffects.BLACK_FLASH.get()) {
            e.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent e) {

        if (e.getEntity() instanceof ServerPlayer flySp) {
            updateFly(flySp);
            InfinitySpellEffect.tick(flySp);
        }

        BlackFlashFlight.tick(e.getEntity());

        if (hasTag(e.getEntity(), "deity")) {
            LivingEntity deity = e.getEntity();
            if (deity instanceof ServerPlayer sp) {
                if (sp.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 1.0)
                    sp.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
                resetFeDelta(sp);
            }
            if (deity.getHealth() <= 0.0F || deity.isDeadOrDying()) {
                deity.deathTime = 0; deity.hurtTime = 0;
                deity.setHealth(deity.getMaxHealth());
            }
            if (deity.deathTime > 0) deity.deathTime = 0;
            if (deity.getHealth() < deity.getMaxHealth()) deity.setHealth(deity.getMaxHealth());
            deity.clearFire();
        } else if (e.getEntity() instanceof ServerPlayer sp) {

            if (sp.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue() > 0.0)
                sp.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0);
        }

        MuryokushoHandler.tickFx(e.getEntity(), e.getEntity().level().getGameTime());
        CurseZoneHandler.tickFx(e.getEntity(), e.getEntity().level().getGameTime());

        if (e.getEntity() instanceof Mob mob) {
            MuryokushoHandler.tryRestoreAi(mob, mob.level().getGameTime());
        }

        JusticeTracker.tickCheck(e.getEntity(), e.getEntity().level().getGameTime());

        if (ResolveHandler.isActive(e.getEntity())) {
            LivingEntity ent = e.getEntity();
            long now = ent.level().getGameTime();
            ResolveHandler.tick(ent, now);

            if (ent instanceof ServerPlayer sp && sp.tickCount % 20 == 0) {
                int secs = ResolveHandler.getRemainingTicks(ent, now) / 20;
                if (secs > 0) sp.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§c⚔ 死战不退 §f" + secs + "§c 秒"), true);
            }
        }

        if (hasTag(e.getEntity(), "revive") && !hasTag(e.getEntity(), "deity")
            && e.getEntity() instanceof ServerPlayer sp
            && !sp.isCreative() && !sp.isSpectator()
            && (sp.getHealth() <= 0.0F || sp.isDeadOrDying())) {
            long now = sp.level().getGameTime();
            int level = TrueNinsEnchantments.bestLevel(sp, "revive");
            int channel = level > 0 ? 1 : 0;
            if (!ReviveHandler.isOnCooldown(sp, now, channel)) {
                ReviveHandler.execute(sp, level);
                ReviveHandler.setCooldown(sp, now, channel);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onDeityAttack(LivingAttackEvent e) {
        if (hasTag(e.getEntity(), "deity")) e.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onDeityHurt(LivingHurtEvent e) {
        if (hasTag(e.getEntity(), "deity")) e.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onDeityDamage(LivingDamageEvent e) {
        if (hasTag(e.getEntity(), "deity")) e.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onDeityDeath(LivingDeathEvent e) {
        if (hasTag(e.getEntity(), "deity")) {
            e.setCanceled(true);
            e.getEntity().deathTime = 0; e.getEntity().hurtTime = 0;
            e.getEntity().setHealth(e.getEntity().getMaxHealth());
            if (e.getEntity() instanceof ServerPlayer sp) sp.onUpdateAbilities();
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public void onDeityKnockback(LivingKnockBackEvent e) {
        if (hasTag(e.getEntity(), "deity")) e.setCanceled(true); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTruemeAttack(LivingAttackEvent e) {
        if (hasTag(e.getEntity(), "deity")) return;
        if (REENTER.get()) return;
        Context ctx = TrueNinsContext.peek();
        if (ctx == null || !ctx.active) return;
        e.setCanceled(true);
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide || target.isDeadOrDying()) return;
        TrueDamageSource ts = TrueDamageSource.of(e.getSource(), target);
        REENTER.set(true);
        try {
            clearFeInvulnerability(target);
            float want = ctx.effectiveAmount();
            float before = target.getHealth() + target.getAbsorptionAmount();
            target.hurt(ts, want);
            float after = target.getHealth() + target.getAbsorptionAmount();
            target.invulnerableTime = 0;
            if (!target.isDeadOrDying() && target.getHealth() > 0.0F && before - after < want * 0.5F) {
                target.setHealth(Math.max(0.0F, target.getHealth() - want));
                if (target.getHealth() <= 0.0F) target.die(ts);
            }
        }
        finally { REENTER.set(false); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTruemeHurt(LivingHurtEvent e) {
        Context ctx = TrueNinsContext.peek();
        if (ctx != null && ctx.active) {
            float min = ctx.effectiveAmount() - Math.min(ctx.effectiveAmount(), ctx.preAbsorption);
            if (e.getAmount() < min) e.setAmount(min);
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onMuryokushoAttack(LivingAttackEvent e) {
        if (e.isCanceled() || e.getEntity().level().isClientSide) return;
        LivingEntity wielder = e.getSource().getDirectEntity() instanceof LivingEntity d ? d : null;
        if (wielder == null) return;
        ItemStack weapon = wielder.getMainHandItem();
        boolean byTag = weapon.hasTag() && weapon.getTag().getBoolean("muryokusho");
        int stasis = TrueNinsEnchantments.levelOf(weapon, "muryokusho");
        if (!byTag && stasis <= 0) return;
        long now = wielder.level().getGameTime();
        if (MuryokushoHandler.isOnCooldown(e.getEntity(), now, byTag ? 0 : 1)) return;
        MuryokushoHandler.apply(e.getEntity(), wielder, now,
            stasis > 0 ? TrueNinsConfig.muryokushoTicks() : -1, byTag ? 0 : 1);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMuryokushoInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent e) {
        if (MuryokushoHandler.isDenied(e.getEntity())) e.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMuryokushoItemUse(LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() instanceof Player p && MuryokushoHandler.isDenied(p)) e.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMuryokushoItemToss(net.minecraftforge.event.entity.item.ItemTossEvent e) {
        if (MuryokushoHandler.isDenied(e.getPlayer())) e.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMuryokushoAttackFrom(LivingAttackEvent e) {
        if (e.getSource().getEntity() instanceof Player p && MuryokushoHandler.isDenied(p))
            e.setCanceled(true); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onThornsAttack(LivingAttackEvent e) {
        if (THORNS_REENTER.get() || e.isCanceled()) return;
        LivingEntity victim = e.getEntity();
        if (!hasTag(victim, "thorns") || victim.level().isClientSide) return;

        LivingEntity atk = null;
        if (e.getSource().getEntity() instanceof LivingEntity s) atk = s;
        if (atk == null && e.getSource().getDirectEntity() instanceof LivingEntity d) atk = d;
        if (atk == null || atk == victim) return;

        float reflected = e.getAmount() * TrueNinsConfig.THORNS_REFLECT_PERCENT.get().floatValue();
        if (reflected <= 0F) return;

        THORNS_REENTER.set(true);
        try {
            atk.hurt(victim.damageSources().mobAttack(victim), reflected);
        } catch (Exception ignored) {
        } finally {
            THORNS_REENTER.set(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAdaptationHurt(LivingHurtEvent e) {
        if (!hasTag(e.getEntity(), "adaptation") || e.getEntity().level().isClientSide) return;
        float reduction = TrueNinsConfig.ADAPTATION_REDUCTION_PERCENT.get().floatValue();
        int level = TrueNinsEnchantments.bestLevel(e.getEntity(), "adaptation");
        if (level > 0) reduction = Math.min(0.10F * level, 0.99F);
        float m = AdaptationTracker.recordAndGet(e.getEntity(), e.getSource().getMsgId(),
            e.getEntity().level().getGameTime(), reduction);
        if (m < 1.0F) e.setAmount(e.getAmount() * m);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMitigationHurt(LivingHurtEvent e) {
        if (!hasTag(e.getEntity(), "mitigation") || e.getEntity().level().isClientSide) return;
        e.setAmount(MitigationTracker.applyCap(e.getEntity(), e.getAmount(),
            e.getEntity().level().getGameTime()));
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onAntihealAttack(LivingAttackEvent e) {
        if (e.isCanceled() || e.getEntity().level().isClientSide) return;
        LivingEntity wielder = e.getSource().getDirectEntity() instanceof LivingEntity d ? d : null;
        if (wielder == null) return;
        ItemStack weapon = wielder.getMainHandItem();
        boolean antihealByTag = weapon.hasTag() && weapon.getTag().getBoolean("antiheal");
        if (!antihealByTag && TrueNinsEnchantments.levelOf(weapon, "antiheal") <= 0) return;
        int cd = TrueNinsConfig.ANTIHEAL_COOLDOWN_SECONDS.get();
        if (cd > 0) {
            long now = wielder.level().getGameTime();
            Long last = ANTIHEAL_COOLDOWNS.get(wielder.getUUID());
            if (last != null && now - last < cd * 20L) return;
            ANTIHEAL_COOLDOWNS.put(wielder.getUUID(), now);
        }
        int amp = TrueNinsConfig.ANTIHEAL_AMPLIFIER.get();
        int dur = TrueNinsConfig.ANTIHEAL_DURATION_SECONDS.get();
        float reduction = TrueNinsConfig.ANTIHEAL_LEVELS[Math.min(amp, 9)].get().floatValue();
        AntihealTracker.apply(e.getEntity().getUUID(), e.getEntity().level().getGameTime(), dur, reduction);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onJusticeAttack(LivingAttackEvent e) {
        if (e.isCanceled() || e.getEntity().level().isClientSide) return;
        if (!(e.getSource().getEntity() instanceof LivingEntity wielder)) return;
        if (!hasTag(wielder, "justice")) return;
        LivingEntity target = e.getEntity();
        long now = target.level().getGameTime();
        JusticeTracker.tryApply(target, now);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onScalingHurt(LivingHurtEvent e) {
        if (e.isCanceled()) return;
        if (!(e.getSource().getEntity() instanceof LivingEntity wielder)) return;
        if (!hasTag(wielder, "scaling")) return;
        float pct = TrueNinsConfig.SCALING_PERCENT.get().floatValue();
        float bonus = e.getEntity().getMaxHealth() * pct;
        if (bonus > 0) e.setAmount(e.getAmount() + bonus);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMaliceHurt(LivingHurtEvent e) {
        if (e.isCanceled()) return;
        if (!(e.getSource().getEntity() instanceof LivingEntity wielder)) return;
        if (!hasTag(wielder, "malice")) return;
        float mult = MaliceTracker.getMultiplier(wielder, e.getEntity(),
            e.getEntity().level().getGameTime(), e.getAmount());
        if (mult > 1.0f) e.setAmount(e.getAmount() * mult);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onResolveAttack(LivingAttackEvent e) {
        if (ResolveHandler.isInvulnerable(e.getEntity())) e.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onResolveHurt(LivingHurtEvent e) {
        if (ResolveHandler.isInvulnerable(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onResolveDeath(LivingDeathEvent e) {
        if (e.isCanceled() || e.getEntity().level().isClientSide) return;
        if (e.getEntity() instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) return;
        if (!hasTag(e.getEntity(), "resolve")) return;
        long now = e.getEntity().level().getGameTime();
        boolean byEnchant = TrueNinsEnchantments.bestLevel(e.getEntity(), "resolve") > 0;
        boolean triggered = byEnchant
            ? ResolveHandler.tryTrigger(e.getEntity(), now,
                ResolveHandler.LAST_STAND_TICKS, ResolveHandler.LAST_STAND_END_DAMAGE)
            : ResolveHandler.tryTrigger(e.getEntity(), now);
        if (triggered) e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSiphonDamage(LivingDamageEvent e) {
        if (e.isCanceled()) return;
        if (!(e.getSource().getEntity() instanceof LivingEntity wielder)) return;
        if (!hasTag(wielder, "siphon")) return;
        float pct = TrueNinsConfig.SIPHON_PERCENT.get().floatValue();
        float heal = e.getAmount() * pct;
        if (heal > 0 && wielder.getHealth() < wielder.getMaxHealth()) {
            wielder.heal(heal);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRageHurt(LivingHurtEvent e) {
        if (e.isCanceled()) return;
        if (!(e.getSource().getEntity() instanceof LivingEntity wielder)) return;
        if (!hasTag(wielder, "rage")) return;
        float lostPct = 1f - wielder.getHealth() / wielder.getMaxHealth();
        double bonus;
        if (lostPct >= 0.9f) bonus = TrueNinsConfig.RAGE_90_PERCENT.get();
        else if (lostPct >= 0.7f) bonus = TrueNinsConfig.RAGE_70_PERCENT.get();
        else if (lostPct >= 0.5f) bonus = TrueNinsConfig.RAGE_50_PERCENT.get();
        else return;

        e.setAmount(e.getAmount() * (1f + (float)bonus / 100f));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onReviveDeath(LivingDeathEvent e) {
        if (e.isCanceled() || !(e.getEntity() instanceof Player p) || p.level().isClientSide) return;
        if (p.isCreative() || p.isSpectator() || hasTag(p, "deity")) return;
        if (!ReviveHandler.hasRevive(p)) return;
        long now = p.level().getGameTime();
        int level = TrueNinsEnchantments.bestLevel(p, "revive");
        int channel = level > 0 ? 1 : 0;
        if (ReviveHandler.isOnCooldown(p, now, channel)) return;
        e.setCanceled(true);
        ReviveHandler.execute(p, level);
        ReviveHandler.setCooldown(p, now, channel);
    }

        private static final java.util.Set<java.util.UUID> INFINITY_BILLED = new java.util.HashSet<>();
    private static final java.util.Map<java.util.UUID, Boolean> INFINITY_FALL = new java.util.HashMap<>();
    private static final double INFINITY_RADIUS = 2.0D;

    static boolean infinityActive(LivingEntity entity) {
        return hasTag(entity, "infinity");
    }

    static boolean infinityCharged(LivingEntity entity) {
        if (!(entity instanceof Player p)) return true;
        if (p.getPersistentData().getBoolean(InfinitySpellEffect.KEY_FREE)) return true;
        return p.experienceLevel > 0;
    }

    static void spendInfinity(LivingEntity entity) {
        if (entity instanceof Player p && !p.getPersistentData().getBoolean(InfinitySpellEffect.KEY_FREE)) {
            p.giveExperienceLevels(-1);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onInfinityAttack(LivingAttackEvent e) {
        LivingEntity victim = e.getEntity();
        if (e.isCanceled() || victim.level().isClientSide) return;
        if (!infinityActive(victim)) return;

        if (e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            e.setCanceled(true);
            victim.clearFire();
            return;
        }

        if (!infinityCharged(victim)) return;
        net.minecraft.world.entity.Entity direct = e.getSource().getDirectEntity();
        if (direct instanceof net.minecraft.world.entity.projectile.Projectile proj) {
            if (!INFINITY_BILLED.add(proj.getUUID())) { e.setCanceled(true); return; }
        }
        spendInfinity(victim);
        e.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onInfinityHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        if (e.isCanceled() || victim.level().isClientSide) return;
        if (!infinityActive(victim) || infinityCharged(victim)) return;
        e.setAmount(MitigationTracker.applyCap(victim, e.getAmount(), victim.level().getGameTime()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlackFlash(LivingHurtEvent e) {
        LivingEntity target = e.getEntity();
        if (e.isCanceled() || target.level().isClientSide) return;
        if (!(e.getSource().getEntity() instanceof Player attacker) || attacker == target) return;

        ItemStack weapon = attacker.getMainHandItem();
        int weaponChance = 0;
        if (!weapon.isEmpty()) {
            net.minecraft.nbt.CompoundTag tag = weapon.getTag();
            if (tag != null && tag.contains("blackflash")) weaponChance = tag.getInt("blackflash");
        }

        boolean byZone = CurseZoneHandler.armed(attacker)
            && blackFlashRoll(attacker, TrueNinsConfig.curseZoneBlackFlashChance());
        boolean byWeapon = weaponChance > 0 && blackFlashRoll(attacker, weaponChance);
        if (!byZone && !byWeapon) return;

        e.setAmount(e.getAmount() * TrueNinsConfig.blackFlashDamageMultiplier());

        BlackFlashFlight.launch(target, attacker.position());
        blackFlashFx(target, true);

        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            TNSounds.BLACK_FLASH.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 1.0F);

        if (byZone) {
            CurseZoneHandler.spend(attacker);
            CurseZoneHandler.refund(attacker);
        }

        if (attacker instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§8§l黑 闪 §r§7BLACK FLASH"), true);
        }
    }

    private static boolean blackFlashRoll(Player attacker, int chance) {
        return chance >= 100 || attacker.getRandom().nextInt(100) < chance;
    }

    public static void blackFlashFx(LivingEntity target, boolean burst) {
        if (!(target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        double cx = target.getX();
        double cy = target.getY() + target.getBbHeight() * 0.5D;
        double cz = target.getZ();

        serverLevel.sendParticles(TNParticles.MOTE_CROSS.get(), cx, cy, cz, 1,
            0.0D, 0.0D, 0.0D, 0.0D);

        if (burst && TrueNinsConfig.blackFlashBurstEffects()) {
            serverLevel.sendParticles(TNParticles.FLASH_HALO.get(), cx, cy, cz, 1,
                0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(TNParticles.FLASH_SHARD.get(), cx, cy, cz, 6,
                0.05D, 0.05D, 0.05D, 0.22D);
        }
    }


    @SubscribeEvent
    public void onInfinityTick(LivingEvent.LivingTickEvent e) {
        LivingEntity entity = e.getEntity();
        if (entity.level().isClientSide || !infinityActive(entity)) return;

        if (entity.isOnFire()) entity.clearFire();

        if (INFINITY_BILLED.size() > 512) INFINITY_BILLED.clear();
        net.minecraft.world.phys.AABB box = entity.getBoundingBox().inflate(INFINITY_RADIUS + 1.0D);
        for (net.minecraft.world.entity.projectile.Projectile proj
                : entity.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, box)) {
            if (proj.getOwner() == entity) continue;
            double dist = proj.position().distanceTo(entity.position());
            if (dist > INFINITY_RADIUS) continue;
            if (!infinityCharged(entity)) continue;
            if (INFINITY_BILLED.add(proj.getUUID())) spendInfinity(entity);
            double k = Math.max(0.0D, dist / INFINITY_RADIUS);
            net.minecraft.world.phys.Vec3 v = proj.getDeltaMovement();
            double jitter = (entity.level().random.nextDouble() - 0.5D) * 0.02D;
            proj.setDeltaMovement(v.x * k + jitter, v.y * k - (1.0D - k) * 0.03D, v.z * k + jitter);
            proj.hurtMarked = true;
        }

        if (entity instanceof ServerPlayer sp && !sp.onGround() && sp.fallDistance > 3.0F && sp.getDeltaMovement().y < -0.25D) {
            net.minecraft.world.phys.Vec3 start = sp.position();
            net.minecraft.world.phys.Vec3 end = start.add(0.0D, -3.0D, 0.0D);
            net.minecraft.world.phys.BlockHitResult hit = sp.level().clip(new net.minecraft.world.level.ClipContext(
                start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, sp));
            if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                double gap = start.y - hit.getLocation().y;
                if (gap >= 0.0D && gap < 3.0D && infinityCharged(sp)) {
                    if (INFINITY_FALL.put(sp.getUUID(), Boolean.TRUE) == null) spendInfinity(sp);
                    double k = Math.max(0.05D, gap / 3.0D);
                    net.minecraft.world.phys.Vec3 v = sp.getDeltaMovement();
                    sp.setDeltaMovement(v.x, Math.max(v.y * k, -0.08D), v.z);
                    sp.fallDistance = 0.0F;
                    sp.hurtMarked = true;
                }
            }
        }
        if (entity instanceof ServerPlayer sp2 && sp2.onGround()) INFINITY_FALL.remove(sp2.getUUID());
    }

    private static boolean feInvulLookupDone;
    private static java.lang.reflect.Method feInvulMethod;

    public static void clearFeInvulnerability(LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        if (!feInvulLookupDone) {
            feInvulLookupDone = true;
            try {
                Class<?> c = Class.forName("com.mega.uom.util.entity.PlayerInvulnerableEntityData");
                feInvulMethod = c.getMethod("setInvul", Player.class, boolean.class);
            } catch (Exception ignored) {}
        }
        if (feInvulMethod == null) return;
        try { feInvulMethod.invoke(null, player, Boolean.FALSE); } catch (Exception ignored) {}
    }

    public static void resetFeDelta(LivingEntity sp) {
        if (!feDeltaLookupDone) {
            feDeltaLookupDone = true;
            try {
                Class<?> c = Class.forName("com.mega.uom.util.entity.EntityASMUtil");
                java.lang.reflect.Field f = c.getField("FE_GET_HEALTH_DATA");
                @SuppressWarnings("unchecked")
                var acc = (net.minecraft.network.syncher.EntityDataAccessor<Float>) f.get(null);
                feDeltaAccessor = acc;
            } catch (Exception ignored) {}
        }
        if (feDeltaAccessor != null) {
            try { sp.getEntityData().set(feDeltaAccessor, 0f); } catch (Exception ignored) {}
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        AdaptationTracker.clean(e.getEntity());
        MitigationTracker.clean(e.getEntity());
        ReviveHandler.clean(id);
        MuryokushoHandler.clean(id);
        AntihealTracker.clean(id);
        JusticeTracker.clean(id);
        MaliceTracker.clean(id);
        ResolveHandler.clean(id);
        ANTIHEAL_COOLDOWNS.remove(id);
        SAVED_MAYFLY.remove(id);
    }
    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent e) {
        UUID id = e.getEntity().getUUID();
        AdaptationTracker.clean(e.getEntity());
        MitigationTracker.clean(e.getEntity());
        JusticeTracker.clean(id);
        MaliceTracker.clean(id);
        ResolveHandler.clean(id);
        if (e.getEntity() instanceof Player sp &&
            sp.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue() > 0.0)
            sp.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0);
    }

    @SubscribeEvent
    public void onEntityLeave(net.minecraftforge.event.entity.EntityLeaveLevelEvent e) {
        if (e.getEntity() instanceof LivingEntity living) {
            UUID id = living.getUUID();
            AdaptationTracker.clean(living);
            MitigationTracker.clean(living);
            MuryokushoHandler.clean(id);
            AntihealTracker.clean(id);
            JusticeTracker.clean(id);
            MaliceTracker.clean(id);
            ResolveHandler.clean(id);
        }
    }
}
