package com.truenins.entity.domain;

import com.truenins.TrueNinsMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * Single funnel for every domain end-of-life path, so the three domains cannot drift apart.
 *
 * <p>Expiry is handled by the domains themselves; death, logout, dimension change, level unload and
 * server shutdown are handled here. Together they guarantee that the AI / movement / interaction
 * suppression never outlives the domain that caused it: control is a per-tick decision taken from
 * {@link DomainRegistry}, so once a domain is closed every lock is released on the next tick.
 */
@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID)
public final class DomainLifecycle {

    /**
     * Optional Iron integration hook: installed by the compat layer when Iron's Spellbooks is present,
     * so this class never references an Iron type (no NoClassDefFoundError when Iron is missing).
     */
    @Nullable
    private static Runnable optionalShutdownHook;

    private DomainLifecycle() {}

    public static void setOptionalShutdownHook(@Nullable Runnable hook) {
        optionalShutdownHook = hook;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;
        // Death reset for the -10% maximum health cut.
        if (VoidHealthPenalty.hasPenalty(entity)) VoidHealthPenalty.clearOnDeath(entity);
        // A dead caster's domains are torn down completely, whatever they still had queued.
        DomainRegistry.closeOwnedBy(entity.getUUID(), "owner-death");
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        // Covers login, respawn and dimension change: re-attach the saved health cut.
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) VoidHealthPenalty.reattach(player);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        // Belt and braces for the death reset: a respawned player never inherits the cut.
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) VoidHealthPenalty.clearOnDeath(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DomainRegistry.closeOwnedBy(event.getEntity().getUUID(), "logout");
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Domains are level bound and not saved, so a caster leaving the level closes them here.
        DomainRegistry.closeOwnedBy(event.getEntity().getUUID(), "dimension-change");
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) DomainRegistry.forgetLevel(level);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DomainRegistry.closeAll("server-stopping");
        if (optionalShutdownHook != null) optionalShutdownHook.run();
    }
}
