package com.truenins.spell.domain;

import com.truenins.TrueNinsMod;
import com.truenins.entity.domain.DomainRegistry;
import com.truenins.entity.domain.VoidDomainEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Void domain suppression query.
 *
 * <p>Server side the answer comes from the per-tick sweep in {@link DomainRegistry}, so one domain runs
 * one AABB query instead of every entity running a 128 block query. The client keeps its own local query
 * because no sweep exists there.
 *
 * <p>Suppression stays a per-tick decision and never a NoAI flag, so leaving the domain, the domain
 * ending, the caster dying or the entity unloading all restore normal behaviour on the very next tick.
 */
@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID)
public final class VoidControl {
    private record Result(long tick, boolean affected) {}
    private static final java.util.Map<LivingEntity, Result> CLIENT_CACHE = new java.util.WeakHashMap<>();
    private VoidControl() {}

    public static boolean isAffected(LivingEntity entity) {
        return isSuppressed(entity);
    }

    public static boolean isSuppressed(LivingEntity entity) {
        if (!isSubject(entity)) return false;
        if (entity.level() instanceof ServerLevel) return DomainRegistry.isSuppressedBy(entity, DomainRegistry.Kind.VOID);
        return queryClient(entity);
    }

    private static boolean isSubject(LivingEntity entity) {
        if (!entity.isAlive()) return false;
        if (!(entity instanceof Player) && !(entity instanceof Enemy)) return false;
        return !(entity instanceof Player player && player.isSpectator());
    }

    /** Client fallback: the client never runs the server sweep, so it keeps the local radius query. */
    private static synchronized boolean queryClient(LivingEntity entity) {
        long now = entity.level().getGameTime();
        Result cached = CLIENT_CACHE.get(entity);
        if (cached != null && cached.tick == now) return cached.affected;
        boolean affected = false;
        for (VoidDomainEntity domain : entity.level().getEntitiesOfClass(VoidDomainEntity.class,
                entity.getBoundingBox().inflate(128))) {
            if (domain.affects(entity)) { affected = true; break; }
        }
        CLIENT_CACHE.put(entity, new Result(now, affected));
        return affected;
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isSuppressed(entity)) return;
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;
        entity.stopUsingItem();
        if (entity.isPassenger()) entity.stopRiding();
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setJumping(false);
        }
        if (entity instanceof net.minecraft.server.level.ServerPlayer player
                && player.containerMenu != player.inventoryMenu) player.closeContainer();
    }
}
