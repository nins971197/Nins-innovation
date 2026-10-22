package com.truenins.spell.domain;

import com.truenins.TrueNinsMod;
import com.truenins.entity.domain.DomainRegistry;
import com.truenins.entity.domain.JudgmentDomainEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Temporary non-violence control. Does not write NoAI NBT and does not lock the camera. */
@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID)
public final class JudgmentControl {
    private JudgmentControl() {}

    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        release(event.getEntity());
    }
    @SubscribeEvent public static void dimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        release(event.getEntity());
    }
    private static void release(net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer p)) return;
        // Caster side: the registry tears down everything this player still owned (all three domains).
        DomainRegistry.closeOwnedBy(p.getUUID(), "participant-left");
        // Defendant side: a trial cannot stay open once one of its two participants is gone.
        for (var level : p.server.getAllLevels())
            for (var e : com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))
                if (e instanceof JudgmentDomainEntity d && d.participates(p.getUUID())) d.closeDomain("participant-left");
    }

    /** Membership predicate for damage and casting rules; only runs on attacks and casts, never per tick. */
    public static boolean isAffected(LivingEntity e) {
        if (!e.isAlive()) return false;
        return !e.level().getEntitiesOfClass(JudgmentDomainEntity.class, e.getBoundingBox().inflate(36), d -> d.affects(e)).isEmpty();
    }

    /** Control predicate used by the movement / AI / packet mixins; registry backed on the server. */
    public static boolean isSuppressed(LivingEntity e) {
        if (e.level() instanceof ServerLevel) return DomainRegistry.isSuppressedBy(e, DomainRegistry.Kind.JUDGMENT);
        return isAffected(e);
    }

    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (!isSuppressed(e)) return;
        e.setDeltaMovement(Vec3.ZERO); e.fallDistance = 0; e.stopUsingItem();
        if (e.isPassenger()) e.stopRiding();
        if (e instanceof Mob m) { m.getNavigation().stop(); m.setJumping(false); }
        if (e instanceof net.minecraft.server.level.ServerPlayer p && p.containerMenu != p.inventoryMenu) p.closeContainer();
    }
    @SubscribeEvent public static void nonviolence(LivingAttackEvent event) {
        // The verdict is exempt. It is the sole permitted attack inside a trial.
        if (event.getSource().getDirectEntity() instanceof JudgmentDomainEntity) return;
        if (isAffected(event.getEntity()) ||
                (event.getSource().getEntity() instanceof LivingEntity attacker && isAffected(attacker))) {
            event.setCanceled(true);
        }
    }
}
