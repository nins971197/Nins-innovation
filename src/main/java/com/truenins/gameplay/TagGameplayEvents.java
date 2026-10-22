package com.truenins.gameplay;

import com.truenins.TrueNinsConfig;
import com.truenins.command.TrueNinsCommand;
import com.truenins.effect.BlackFlashFlight;
import com.truenins.gameplay.compat.ThirdPartyCompat;
import com.truenins.gameplay.damage.TrueDamageSource;
import com.truenins.gameplay.state.AdaptationTracker;
import com.truenins.gameplay.state.AntihealTracker;
import com.truenins.gameplay.state.JudgmentPenalty;
import com.truenins.gameplay.state.JusticeTracker;
import com.truenins.gameplay.state.MaliceTracker;
import com.truenins.gameplay.state.MitigationTracker;
import com.truenins.gameplay.state.ResolveHandler;
import com.truenins.gameplay.state.ReviveHandler;
import com.truenins.gameplay.state.StunHandler;
import com.truenins.gameplay.tag.TrueNinsContext;
import com.truenins.gameplay.tag.TrueninsChannels;
import com.truenins.register.TNMobEffects;
import com.truenins.register.TNParticles;
import com.truenins.register.TNSounds;
import com.truenins.spell.CurseZoneHandler;
import com.truenins.spell.InfinitySpellEffect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gameplay listeners for the truenins keywords.
 *
 * <p>Keyword sources are resolved through {@link TrueninsChannels}, so a keyword
 * carried by an NBT marker and by the same-name enchantment are two independent
 * channels that are timed separately.</p>
 */
public final class TagGameplayEvents {

    private static final UUID DEITY_KNOCKBACK_ID = UUID.fromString("fd570e18-46a1-48bc-9d39-61ad3b9aa420");

    /** Persistent fly bookkeeping: the grant flag and the player's own mayfly value. */
    private static final String FLY_SAVED_TAG = "truenins_fly_saved";
    private static final String FLY_VALUE_TAG = "truenins_fly_value";

    private static final Map<UUID, long[]> ANTIHEAL_COOLDOWNS = new HashMap<>();

    private static final Set<UUID> INFINITY_BILLED = new HashSet<>();
    private static final Map<UUID, Boolean> INFINITY_FALL = new HashMap<>();
    private static final double INFINITY_RADIUS = 2.0D;

    // ---------------------------------------------------------------- commands

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TrueNinsCommand.register(event.getDispatcher(), event.getBuildContext());
    }

    public static boolean hasTag(LivingEntity entity, String tag) {
        return entity != null && TrueninsChannels.has(entity, tag);
    }

    // ------------------------------------------------------------------- fly

    @SubscribeEvent
    public void onEquipmentChange(LivingEquipmentChangeEvent event) {
        TrueninsChannels.invalidate(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer player) updateFly(player);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) updateFly(player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (InfinitySpellEffect.isActive(player)) InfinitySpellEffect.stop(player);
        updateFly(player);
    }

    @SubscribeEvent
    public void onPlayerDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TrueninsChannels.invalidate(player);
        updateFly(player);
    }

    @SubscribeEvent
    public void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TrueninsChannels.invalidate(player);
        updateFly(player);
    }

    private static void updateFly(ServerPlayer player) {
        // Creative and spectator already own their flight; never touch those abilities.
        if (player.isCreative() || player.isSpectator()) return;
        applyFlyGrant(player, TrueninsChannels.has(player, "fly"));
    }

    /**
     * Grants or revokes flight from the fly keyword. The player's own mayfly value is
     * recorded on the player data the first time it is overridden, so a logout, a
     * death or a server restart can always restore the real value instead of leaving
     * a permanent grant.
     */
    private static void applyFlyGrant(ServerPlayer player, boolean granted) {
        CompoundTag data = player.getPersistentData();
        boolean recorded = data.getBoolean(FLY_SAVED_TAG);
        var abilities = player.getAbilities();

        if (granted) {
            if (!recorded) {
                data.putBoolean(FLY_SAVED_TAG, true);
                data.putBoolean(FLY_VALUE_TAG, abilities.mayfly);
            }
            if (!abilities.mayfly) {
                abilities.mayfly = true;
                player.onUpdateAbilities();
            }
            // Only while airborne: a granted mayfly must not silently disable fall damage.
            if (abilities.flying || player.isFallFlying()) player.fallDistance = 0.0F;
        } else if (recorded) {
            releaseFlyGrant(player, data.getBoolean(FLY_VALUE_TAG));
        }
    }

    private static void releaseFlyGrant(ServerPlayer player, boolean originalMayfly) {
        CompoundTag data = player.getPersistentData();
        data.remove(FLY_SAVED_TAG);
        data.remove(FLY_VALUE_TAG);

        var abilities = player.getAbilities();
        abilities.mayfly = originalMayfly;
        abilities.flying = false;
        player.onUpdateAbilities();
    }

    // ------------------------------------------------------------- immunity

    @SubscribeEvent
    public void onImmunityEffect(MobEffectEvent.Applicable event) {
        MobEffect effect = event.getEffectInstance().getEffect();
        if (effect == TNMobEffects.BLACK_FLASH.get()
            || effect == TNMobEffects.CURSE_ZONE.get()
            || effect == TNMobEffects.MADE_IN_HEAVEN.get()) {
            return;
        }
        if (effect.getCategory() != MobEffectCategory.BENEFICIAL
            && TrueninsChannels.has(event.getEntity(), "immunity")) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlackFlashApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == TNMobEffects.BLACK_FLASH.get()) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    // ------------------------------------------------------------ living tick

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        long now = entity.level().getGameTime();

        if (entity instanceof ServerPlayer player) {
            updateFly(player);
            InfinitySpellEffect.tick(player);
        }

        BlackFlashFlight.tick(entity);

        if (TrueninsChannels.has(entity, "deity")) {
            tickDeity(entity);
        } else if (entity instanceof ServerPlayer player) {
            removeDeityKnockbackResistance(player);
        }

        StunHandler.tickFx(entity, now);
        CurseZoneHandler.tickFx(entity, now);
        if (!entity.level().isClientSide) JudgmentPenalty.tick(entity);
        if (entity instanceof Mob mob) StunHandler.restoreIfExpired(mob, now);

        JusticeTracker.tickCheck(entity, now);
        tickResolve(entity, now);
    }

    private static void tickDeity(LivingEntity deity) {
        if (deity instanceof ServerPlayer player) {
            var resistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (resistance != null && resistance.getModifier(DEITY_KNOCKBACK_ID) == null) {
                resistance.addTransientModifier(new AttributeModifier(DEITY_KNOCKBACK_ID,
                    "truenins_deity_knockback", 1.0D, AttributeModifier.Operation.ADDITION));
            }
            ThirdPartyCompat.resetFeHealthDelta(player);
        }

        if (deity.getHealth() <= 0.0F || deity.isDeadOrDying()) {
            deity.deathTime = 0;
            deity.hurtTime = 0;
            deity.setHealth(deity.getMaxHealth());
        }
        if (deity.deathTime > 0) deity.deathTime = 0;
        if (deity.getHealth() < deity.getMaxHealth()) deity.setHealth(deity.getMaxHealth());
        deity.clearFire();
    }

    private static void tickResolve(LivingEntity entity, long now) {
        if (!ResolveHandler.isActive(entity)) return;
        ResolveHandler.tick(entity, now);

        if (entity instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            int seconds = ResolveHandler.getRemainingTicks(entity, now) / 20;
            if (seconds > 0) {
                player.displayClientMessage(Component.literal("§c\u2694 死战不退 §f" + seconds + "§c 秒"), true);
            }
        }
    }

    private static void removeDeityKnockbackResistance(ServerPlayer player) {
        var resistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance != null) resistance.removeModifier(DEITY_KNOCKBACK_ID);
    }

    // ----------------------------------------------------------------- deity

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeityAttack(LivingAttackEvent event) {
        if (TrueninsChannels.has(event.getEntity(), "deity")) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeityHurt(LivingHurtEvent event) {
        if (TrueninsChannels.has(event.getEntity(), "deity")) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeityDamage(LivingDamageEvent event) {
        if (TrueninsChannels.has(event.getEntity(), "deity")) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!TrueninsChannels.has(entity, "deity")) return;
        event.setCanceled(true);
        entity.deathTime = 0;
        entity.hurtTime = 0;
        entity.setHealth(entity.getMaxHealth());
        if (entity instanceof ServerPlayer player) player.onUpdateAbilities();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeityKnockback(LivingKnockBackEvent event) {
        if (TrueninsChannels.has(event.getEntity(), "deity")) event.setCanceled(true);
    }

    // ----------------------------------------------------------- true damage

    /**
     * Replaces the incoming hit with a true damage hit. The thread local flag is
     * entered and left in this method only; every other damage source keeps its
     * normal bypass behaviour.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTruemeAttack(LivingAttackEvent event) {
        if (event.isCanceled() || TrueNinsContext.inTrueDamage()) return;

        LivingEntity target = event.getEntity();
        if (TrueninsChannels.has(target, "deity")) return;

        TrueNinsContext.Context context = TrueNinsContext.of(event.getSource(), event.getAmount());
        if (!context.active) return;

        // The original hit is only voided once the target really is a valid victim: cancelling it
        // before these checks made every attack on a dead or protected entity a no-op.
        if (target.level().isClientSide || target.isDeadOrDying()) return;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        event.setCanceled(true);

        TrueDamageSource trueDamage = TrueDamageSource.of(event.getSource(), target);
        float wanted = context.effectiveAmount();

        // Clear the hit cooldown before the nested hit, otherwise vanilla drops it as a repeat hit.
        target.invulnerableTime = 0;
        target.hurtTime = 0;

        TrueNinsContext.enterTrueDamage();
        try {
            if (target instanceof Player player) ThirdPartyCompat.clearPlayerInvulnerability(player);
            target.hurt(trueDamage, wanted);
            target.invulnerableTime = 0;

            // Top the hit up only when the pipeline never reported a landed amount, meaning the
            // nested hurt was cancelled before dealing damage. Anything the pipeline applied already
            // counts - absorption, damage reduction, a revive that restored the health or a resolve
            // that left one point - so a target can never be hit twice by one attack.
            if (TrueNinsContext.landed() < 0.0F
                    && !target.isDeadOrDying() && target.getHealth() > 0.0F) {
                target.setHealth(Math.max(0.0F, target.getHealth() - wanted));
                if (target.getHealth() <= 0.0F) target.die(trueDamage);
            }
        } finally {
            TrueNinsContext.exitTrueDamage();
        }
    }

    /**
     * Records what the nested true damage hit really landed. The source instance check is
     * deliberate: the thread local window alone would also bless damage nested inside the hit.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTrueDamageLand(LivingDamageEvent event) {
        if (event.isCanceled() || !TrueNinsContext.inTrueDamage()) return;
        if (!(event.getSource() instanceof TrueDamageSource)) return;
        TrueNinsContext.recordLanded(event.getAmount());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTruemeHurt(LivingHurtEvent event) {
        TrueNinsContext.Context context = TrueNinsContext.of(event.getSource(), event.getAmount());
        if (!context.active) return;

        LivingEntity target = event.getEntity();
        float amount = context.effectiveAmount();
        float minimum = amount - Math.min(amount, target.getAbsorptionAmount());
        if (event.getAmount() < minimum) event.setAmount(minimum);
    }

    // ------------------------------------------------------------------ stun

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onStunAttack(LivingAttackEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity wielder)) return;

        int channels = TrueninsChannels.itemMask(wielder.getMainHandItem(), "stun");
        if (channels == 0) return;

        StunHandler.applyStun(event.getEntity(), wielder.level().getGameTime(), channels);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onStunInteract(PlayerInteractEvent event) {
        if (StunHandler.isDenied(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onStunItemUse(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && StunHandler.isDenied(player)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onStunItemToss(ItemTossEvent event) {
        if (StunHandler.isDenied(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onStunAttackFrom(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof Player player && StunHandler.isDenied(player)) {
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------- judgment confiscation

    /**
     * Light verdict of Deadly Sentencing: everything the sentenced defendant deals is
     * cut to 70%. One hook covers melee, arrows and spells, so the penalty means the
     * same thing for a zombie, a skeleton and a player.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onConfiscationHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0.0F) return;
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        if (attacker == null || attacker == event.getEntity()) return;
        if (!JudgmentPenalty.isSentenced(attacker)) return;
        event.setAmount(event.getAmount() * JudgmentPenalty.DAMAGE_MULTIPLIER);
    }

    // ---------------------------------------------------------------- thorns

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onThornsAttack(LivingAttackEvent event) {
        if (event.isCanceled() || TrueNinsContext.inReflect()) return;

        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !TrueninsChannels.has(victim, "thorns")) return;

        LivingEntity attacker = null;
        if (event.getSource().getEntity() instanceof LivingEntity source) attacker = source;
        if (attacker == null && event.getSource().getDirectEntity() instanceof LivingEntity direct) attacker = direct;
        if (attacker == null || attacker == victim) return;

        float reflected = event.getAmount() * TrueNinsConfig.THORNS_REFLECT_PERCENT.get().floatValue();
        if (reflected <= 0.0F) return;

        TrueNinsContext.enterReflect();
        try {
            attacker.hurt(victim.damageSources().mobAttack(victim), reflected);
        } catch (Exception ignored) {
            // A hostile damage pipeline must never break the triggering hit.
        } finally {
            TrueNinsContext.exitReflect();
        }
    }

    // ------------------------------------------------- defensive keywords

    /**
     * Adaptation stacks per channel; both channels multiply their own reduction into
     * the same hit instead of replacing each other.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onAdaptationHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        int channels = TrueninsChannels.mask(entity, "adaptation");
        if (channels == 0) return;

        String messageId = event.getSource().getMsgId();
        long now = entity.level().getGameTime();
        float multiplier = 1.0F;

        if ((channels & TrueninsChannels.NBT_BIT) != 0) {
            multiplier *= AdaptationTracker.recordAndGet(entity, messageId, now, TrueninsChannels.NBT);
        }
        if ((channels & TrueninsChannels.ENCHANT_BIT) != 0) {
            int level = TrueninsChannels.level(entity, "adaptation");
            float enchantMultiplier = Math.min(0.10F * level, 0.99F);
            multiplier *= AdaptationTracker.recordAndGet(entity, messageId, now, TrueninsChannels.ENCHANT,
                enchantMultiplier);
        }

        if (multiplier < 1.0F) event.setAmount(event.getAmount() * multiplier);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMitigationHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        int channels = TrueninsChannels.mask(entity, "mitigation");
        if (channels == 0) return;

        long now = entity.level().getGameTime();
        float amount = event.getAmount();
        for (int channel = 0; channel < 2; channel++) {
            if ((channels & TrueninsChannels.bit(channel)) == 0) continue;
            amount = MitigationTracker.applyCap(entity, amount, now, channel);
        }
        event.setAmount(amount);
    }

    // --------------------------------------------------- anti heal / justice

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onAntihealAttack(LivingAttackEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity wielder)) return;

        int channels = TrueninsChannels.itemMask(wielder.getMainHandItem(), "antiheal");
        if (channels == 0) return;

        long now = wielder.level().getGameTime();
        long cooldownTicks = TrueNinsConfig.ANTIHEAL_COOLDOWN_SECONDS.get() * 20L;
        int duration = TrueNinsConfig.ANTIHEAL_DURATION_SECONDS.get();
        float reduction = TrueNinsConfig.ANTIHEAL_LEVELS[
            Math.min(TrueNinsConfig.ANTIHEAL_AMPLIFIER.get(), 9)].get().floatValue();

        long[] marks = ANTIHEAL_COOLDOWNS.computeIfAbsent(wielder.getUUID(), key -> new long[2]);
        for (int channel = 0; channel < 2; channel++) {
            if ((channels & TrueninsChannels.bit(channel)) == 0) continue;
            if (cooldownTicks > 0L && marks[channel] != 0L && now - marks[channel] < cooldownTicks) continue;
            marks[channel] = now;
            AntihealTracker.apply(event.getEntity().getUUID(), now, duration, reduction, channel);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onJusticeAttack(LivingAttackEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity wielder)) return;

        int channels = TrueninsChannels.mask(wielder, "justice");
        if (channels == 0) return;

        LivingEntity target = event.getEntity();
        JusticeTracker.tryApply(target, target.level().getGameTime(), channels);
    }

    // ------------------------------------------------- offensive multipliers

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onScalingHurt(LivingHurtEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity wielder)) return;
        if (!TrueninsChannels.has(wielder, "scaling")) return;

        float percent = TrueNinsConfig.SCALING_PERCENT.get().floatValue();
        float bonus = event.getEntity().getMaxHealth() * percent;
        if (bonus > 0.0F) event.setAmount(event.getAmount() + bonus);
    }

    /**
     * Malice stacks per channel, but only the strongest channel multiplier is applied
     * so a hit is never multiplied twice by the same keyword.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMaliceHurt(LivingHurtEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity wielder)) return;

        int channels = TrueninsChannels.mask(wielder, "malice");
        if (channels == 0) return;

        LivingEntity target = event.getEntity();
        long now = target.level().getGameTime();
        float multiplier = 1.0F;
        for (int channel = 0; channel < 2; channel++) {
            if ((channels & TrueninsChannels.bit(channel)) == 0) continue;
            multiplier = Math.max(multiplier,
                MaliceTracker.getMultiplier(wielder, target, now, event.getAmount(), channel));
        }
        if (multiplier > 1.0F) event.setAmount(event.getAmount() * multiplier);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSiphonDamage(LivingDamageEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity wielder)) return;
        if (!TrueninsChannels.has(wielder, "siphon")) return;

        float heal = event.getAmount() * TrueNinsConfig.SIPHON_PERCENT.get().floatValue();
        if (heal > 0.0F && wielder.getHealth() < wielder.getMaxHealth()) wielder.heal(heal);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRageHurt(LivingHurtEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity wielder)) return;
        if (!TrueninsChannels.has(wielder, "rage")) return;

        float lostPercent = 1.0F - wielder.getHealth() / wielder.getMaxHealth();
        double bonus;
        if (lostPercent >= 0.9F) bonus = TrueNinsConfig.RAGE_90_PERCENT.get();
        else if (lostPercent >= 0.7F) bonus = TrueNinsConfig.RAGE_70_PERCENT.get();
        else if (lostPercent >= 0.5F) bonus = TrueNinsConfig.RAGE_50_PERCENT.get();
        else return;

        event.setAmount(event.getAmount() * (1.0F + (float) bonus / 100.0F));
    }

    // ------------------------------------------------------ last stand / revive

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onResolveAttack(LivingAttackEvent event) {
        if (ResolveHandler.isInvulnerable(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onResolveHurt(LivingHurtEvent event) {
        if (ResolveHandler.isInvulnerable(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onResolveDeath(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) return;

        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) return;

        int channels = TrueninsChannels.mask(entity, "resolve");
        if (channels == 0) return;

        if (ResolveHandler.tryTrigger(entity, entity.level().getGameTime(), channels)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onReviveDeath(LivingDeathEvent event) {
        if (event.isCanceled() || (event.getEntity() instanceof Player player
            && (player.isCreative() || player.isSpectator()))) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (TrueninsChannels.has(player, "deity")) return;

        int channels = TrueninsChannels.mask(player, "revive");
        if (channels == 0) return;

        if (ReviveHandler.tryRevive(player, player.level().getGameTime(), channels)) event.setCanceled(true);
    }

    // -------------------------------------------------------------- infinity

    static boolean infinityActive(LivingEntity entity) {
        return TrueninsChannels.has(entity, "infinity");
    }

    static boolean infinityCharged(LivingEntity entity) {
        if (!(entity instanceof Player player)) return true;
        if (player.getPersistentData().getBoolean(InfinitySpellEffect.KEY_FREE)) return true;
        return player.experienceLevel > 0;
    }

    static void spendInfinity(LivingEntity entity) {
        if (entity instanceof Player player
            && !player.getPersistentData().getBoolean(InfinitySpellEffect.KEY_FREE)) {
            player.giveExperienceLevels(-1);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onInfinityAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.isCanceled() || victim.level().isClientSide) return;
        if (!infinityActive(victim)) return;

        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            victim.clearFire();
            return;
        }

        if (!infinityCharged(victim)) return;
        if (event.getSource().getDirectEntity() instanceof Projectile projectile
            && !INFINITY_BILLED.add(projectile.getUUID())) {
            event.setCanceled(true);
            return;
        }
        spendInfinity(victim);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onInfinityHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.isCanceled() || victim.level().isClientSide) return;
        if (!infinityActive(victim) || infinityCharged(victim)) return;

        int channels = TrueninsChannels.mask(victim, "infinity");
        long now = victim.level().getGameTime();
        float amount = event.getAmount();
        for (int channel = 0; channel < 2; channel++) {
            if ((channels & TrueninsChannels.bit(channel)) == 0) continue;
            amount = MitigationTracker.applyCap(victim, amount, now, channel);
        }
        event.setAmount(amount);
    }

    // ------------------------------------------------------------ black flash

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlackFlash(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (event.isCanceled() || target.level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof Player attacker) || attacker == target) return;

        int weaponChance = 0;
        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.isEmpty()) {
            CompoundTag tag = weapon.getTag();
            if (tag != null && tag.contains("blackflash")) weaponChance = tag.getInt("blackflash");
        }

        boolean byZone = CurseZoneHandler.armed(attacker)
            && blackFlashRoll(attacker, TrueNinsConfig.curseZoneBlackFlashChance());
        boolean byWeapon = weaponChance > 0 && blackFlashRoll(attacker, weaponChance);
        if (!byZone && !byWeapon) return;

        event.setAmount(event.getAmount() * TrueNinsConfig.blackFlashDamageMultiplier());
        BlackFlashFlight.launch(target, attacker.position());
        blackFlashFx(target, true);
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            TNSounds.BLACK_FLASH.get(), SoundSource.PLAYERS, 1.4F, 1.0F);

        if (byZone) {
            CurseZoneHandler.spend(attacker);
            CurseZoneHandler.refund(attacker);
        }
        if (attacker instanceof ServerPlayer player) {
            player.displayClientMessage(Component.literal("§8§l黑 闪 §r§7BLACK FLASH"), true);
        }
    }

    private static boolean blackFlashRoll(Player attacker, int chance) {
        return chance >= 100 || attacker.getRandom().nextInt(100) < chance;
    }

    public static void blackFlashFx(LivingEntity target, boolean burst) {
        if (!(target.level() instanceof ServerLevel level)) return;
        double centerX = target.getX();
        double centerY = target.getY() + target.getBbHeight() * 0.5D;
        double centerZ = target.getZ();

        level.sendParticles(TNParticles.MOTE_CROSS.get(), centerX, centerY, centerZ, 1,
            0.0D, 0.0D, 0.0D, 0.0D);

        if (!burst || !TrueNinsConfig.blackFlashBurstEffects()) return;
        level.sendParticles(TNParticles.FLASH_HALO.get(), centerX, centerY, centerZ, 1,
            0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(TNParticles.FLASH_SHARD.get(), centerX, centerY, centerZ, 6,
            0.05D, 0.05D, 0.05D, 0.22D);
    }

    // ------------------------------------------------------- infinity ticks

    @SubscribeEvent
    public void onInfinityTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !infinityActive(entity)) return;

        if (entity.isOnFire()) entity.clearFire();
        deflectProjectiles(entity);

        if (entity instanceof ServerPlayer player) slowFall(player);
    }

    private static void deflectProjectiles(LivingEntity entity) {
        if (INFINITY_BILLED.size() > 512) INFINITY_BILLED.clear();

        AABB box = entity.getBoundingBox().inflate(INFINITY_RADIUS + 1.0D);
        for (Projectile projectile : entity.level().getEntitiesOfClass(Projectile.class, box)) {
            if (projectile.getOwner() == entity) continue;
            double distance = projectile.position().distanceTo(entity.position());
            if (distance > INFINITY_RADIUS) continue;
            if (!infinityCharged(entity)) continue;

            if (INFINITY_BILLED.add(projectile.getUUID())) spendInfinity(entity);

            double damping = Math.max(0.0D, distance / INFINITY_RADIUS);
            Vec3 movement = projectile.getDeltaMovement();
            double jitter = (entity.level().random.nextDouble() - 0.5D) * 0.02D;
            projectile.setDeltaMovement(
                movement.x * damping + jitter,
                movement.y * damping - (1.0D - damping) * 0.03D,
                movement.z * damping + jitter);
            projectile.hurtMarked = true;
        }
    }

    private static void slowFall(ServerPlayer player) {
        if (!player.onGround() && player.fallDistance > 3.0F && player.getDeltaMovement().y < -0.25D) {
            Vec3 start = player.position();
            Vec3 end = start.add(0.0D, -3.0D, 0.0D);
            BlockHitResult hit = player.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

            if (hit.getType() != HitResult.Type.MISS) {
                double gap = start.y - hit.getLocation().y;
                if (gap >= 0.0D && gap < 3.0D && infinityCharged(player)) {
                    if (INFINITY_FALL.put(player.getUUID(), Boolean.TRUE) == null) spendInfinity(player);
                    double damping = Math.max(0.05D, gap / 3.0D);
                    Vec3 movement = player.getDeltaMovement();
                    player.setDeltaMovement(movement.x, Math.max(movement.y * damping, -0.08D), movement.z);
                    player.fallDistance = 0.0F;
                    player.hurtMarked = true;
                }
            }
        }
        if (player.onGround()) INFINITY_FALL.remove(player.getUUID());
    }

    // ----------------------------------------------------------- third party

    public static void clearFeInvulnerability(LivingEntity entity) {
        if (entity instanceof Player player) ThirdPartyCompat.clearPlayerInvulnerability(player);
    }

    public static void resetFeDelta(LivingEntity entity) {
        ThirdPartyCompat.resetFeHealthDelta(entity);
    }

    // --------------------------------------------------------------- cleanup

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        CompoundTag data = player.getPersistentData();
        if (player instanceof ServerPlayer serverPlayer) {
            if (!serverPlayer.isCreative() && !serverPlayer.isSpectator() && data.getBoolean(FLY_SAVED_TAG)) {
                releaseFlyGrant(serverPlayer, data.getBoolean(FLY_VALUE_TAG));
            }
            removeDeityKnockbackResistance(serverPlayer);
        }

        UUID id = player.getUUID();
        TrueninsChannels.clean(id);
        AdaptationTracker.clean(id);
        MitigationTracker.clean(id);
        ReviveHandler.clean(id);
        StunHandler.clean(player);
        AntihealTracker.clean(id);
        JusticeTracker.clean(player);
        MaliceTracker.clean(id);
        ResolveHandler.clean(id);
        ANTIHEAL_COOLDOWNS.remove(id);

        com.truenins.entity.orb.BlueOrbEntity.FAILED_CHARGES.remove(id);
        com.truenins.entity.orb.BlueOrbEntity charging = com.truenins.entity.orb.BlueOrbEntity.CHARGING.remove(id);
        if (charging != null) charging.discard();
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        UUID id = entity.getUUID();
        AdaptationTracker.clean(id);
        MitigationTracker.clean(id);
        StunHandler.clean(entity);
        AntihealTracker.clean(id);
        ANTIHEAL_COOLDOWNS.remove(id);
        JudgmentPenalty.clear(entity);
        justiceCleanup(entity);
        MaliceTracker.clean(id);
        ResolveHandler.clean(id);
        if (entity instanceof ServerPlayer player) removeDeityKnockbackResistance(player);
    }

    @SubscribeEvent
    public void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        UUID id = entity.getUUID();
        TrueninsChannels.clean(id);
        AdaptationTracker.clean(id);
        MitigationTracker.clean(id);
        // Cache only: the recorded AI restore point lives in NBT and must survive a chunk unload,
        // otherwise walking away from a stunned mob would end its stun early. The recharge is kept
        // too, so unloading a mob cannot be used to reset the stun cooldown.
        StunHandler.cleanActive(id);
        AntihealTracker.clean(id);
        justiceCleanup(entity);
        MaliceTracker.clean(id);
        ResolveHandler.clean(id);
    }

    /**
     * A stunned mob can be unloaded while it still owns an AI flag; restore it as soon
     * as the entity joins a level again instead of waiting for its next tick.
     */
    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob) StunHandler.restoreIfExpired(mob, mob.level().getGameTime());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) TrueNinsContext.reset();
    }

    private static void justiceCleanup(LivingEntity entity) {
        JusticeTracker.clean(entity);
        JusticeTracker.clean(entity.getUUID());
    }
}
