package com.truenins.entity.domain;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Central registry for the three domains: one place for mutual exclusion, control takeover, the per
 * tick batch sweep and the end-of-life path.
 *
 * <p>Why this class exists: every caster could keep all of their domains open at once, teardown logic
 * lived in three unrelated classes, and every consumer ran its own radius query (VoidControl did a
 * 128 block lookup per entity per query). Here instead:
 * <ul>
 *   <li>one active domain per caster: opening a new domain runs the previous one through its complete
 *       teardown path ({@link Domain#closeDomain(String)});</li>
 *   <li>one control owner per entity: the newest domain wins, so the older domain automatically stops
 *       controlling that entity on the next sweep;</li>
 *   <li>one sweep per level and game tick: each domain performs a single AABB query, and all hot
 *       consumers just read the resulting map.</li>
 * </ul>
 *
 * <p>All state is only mutated from the server main thread. {@link #controllingDomain(Entity)} refuses
 * to answer for off-thread callers so a packet handler can never observe a half-built sweep.
 */
public final class DomainRegistry {

    public enum Kind { VOID, JUDGMENT, SHINGAN }

    /** Minimal contract a domain entity obeys to take part in the registry. */
    public interface Domain {

        Kind domainKind();

        /** Caster UUID, or {@code null} when the domain has no owner. */
        @Nullable
        UUID domainOwnerId();

        Entity domainEntity();

        /** Registration order; a higher value means the domain was opened later. */
        long domainSeq();

        void setDomainSeq(long seq);

        /** Whether this kind really suppresses AI / movement / interaction while it holds control. */
        boolean domainSuppressesControl();

        /** Whether this domain currently claims control of the given entity. */
        boolean controlsEntity(LivingEntity entity);

        /** One batched AABB query per tick, handing every claimed entity to {@code out}. */
        void collectControlled(ServerLevel level, Consumer<LivingEntity> out);

        /** Idempotent complete teardown: expiry, caster death, logout, dimension change, shutdown, takeover. */
        void closeDomain(String reason);

        /** True from the moment the domain stops taking control until it is fully gone. */
        boolean domainClosing();
    }

    private static final class LevelState {
        long tick = Long.MIN_VALUE;
        final List<Domain> domains = new ArrayList<>();
        final Map<Integer, Domain> control = new HashMap<>();
    }

    private static final Map<ResourceKey<Level>, LevelState> LEVELS = new HashMap<>();
    private static final Map<UUID, Domain> BY_OWNER = new HashMap<>();
    private static final List<Domain> ALL = new ArrayList<>();
    /**
     * Level a domain was opened in. Stored at registration time because the domain entity can be
     * moved to another dimension before it unregisters, and looking the state up by the entity's
     * current dimension silently leaked the old entry.
     */
    private static final Map<Domain, ResourceKey<Level>> LEVEL_OF = new HashMap<>();
    private static long nextSeq = 1L;

    private DomainRegistry() {}

    /** Registers a freshly opened domain; a previous domain of the same caster is closed first. */
    public static void register(Domain domain) {
        if (domain.domainEntity().level().isClientSide) return;
        if (ALL.contains(domain)) return;
        UUID owner = domain.domainOwnerId();
        if (owner != null) {
            Domain previous = BY_OWNER.get(owner);
            // Mutual exclusion: the previous domain goes through its own complete teardown instead of
            // being left to expire on its own next to the new one.
            if (previous != null && previous != domain) previous.closeDomain("superseded-by-" + domain.domainKind());
        }
        domain.setDomainSeq(nextSeq++);
        ALL.add(domain);
        if (owner != null) BY_OWNER.put(owner, domain);
        LevelState state = state(domain.domainEntity().level());
        LEVEL_OF.put(domain, domain.domainEntity().level().dimension());
        if (!state.domains.contains(domain)) state.domains.add(domain);
        state.tick = Long.MIN_VALUE; // force a fresh sweep so the new domain takes control immediately
    }

    /** Idempotent removal; called from {@link Domain#closeDomain(String)} and from entity removal. */
    public static void unregister(Domain domain) {
        ALL.remove(domain);
        UUID owner = domain.domainOwnerId();
        if (owner != null) BY_OWNER.remove(owner, domain);
        ResourceKey<Level> key = LEVEL_OF.remove(domain);
        LevelState state = key != null ? LEVELS.get(key) : null;
        if (state == null) return;
        state.domains.remove(domain);
        state.control.values().removeIf(claimed -> claimed == domain);
    }

    /** Closes every domain owned by this caster: death, logout and dimension change all funnel here. */
    public static void closeOwnedBy(@Nullable UUID ownerId, String reason) {
        if (ownerId == null) return;
        Domain owned = BY_OWNER.get(ownerId);
        if (owned != null) owned.closeDomain(reason);
        // Defensive: an older save or an abnormal path may have left more than one domain behind.
        for (Domain domain : new ArrayList<>(ALL)) {
            if (ownerId.equals(domain.domainOwnerId())) domain.closeDomain(reason);
        }
    }

    public static void closeAllIn(Level level, String reason) {
        LevelState state = LEVELS.get(level.dimension());
        if (state == null) return;
        for (Domain domain : new ArrayList<>(state.domains)) domain.closeDomain(reason);
    }

    public static void closeAll(String reason) {
        for (Domain domain : new ArrayList<>(ALL)) domain.closeDomain(reason);
    }

    /** Drops bookkeeping for a level that is being unloaded; entities there are already being torn down. */
    public static void forgetLevel(Level level) {
        LevelState state = LEVELS.remove(level.dimension());
        if (state == null) return;
        for (Domain domain : new ArrayList<>(state.domains)) {
            ALL.remove(domain);
            UUID owner = domain.domainOwnerId();
            if (owner != null) BY_OWNER.remove(owner, domain);
            LEVEL_OF.remove(domain);
        }
        state.domains.clear();
        state.control.clear();
    }

    @Nullable
    public static Domain controllingDomain(Entity entity) {
        if (entity == null) return null;
        if (!(entity.level() instanceof ServerLevel level)) return null;
        // Only the server thread may decide control; a stray off-thread packet handler must not race the sweep.
        if (!level.getServer().isSameThread()) return null;
        LevelState state = sweep(level);
        Domain domain = state.control.get(entity.getId());
        if (domain == null) return null;
        if (domain.domainClosing() || domain.domainEntity().isRemoved()) {
            state.control.remove(entity.getId());
            return null;
        }
        return domain;
    }

    public static boolean isControlled(Entity entity) {
        return controllingDomain(entity) != null;
    }

    /** True when the controlling domain actively suppresses the entity (shingan claims but does not suppress). */
    public static boolean isSuppressed(Entity entity) {
        Domain domain = controllingDomain(entity);
        return domain != null && domain.domainSuppressesControl();
    }

    /** Same as {@link #isSuppressed(Entity)} but restricted to one domain kind. */
    public static boolean isSuppressedBy(Entity entity, Kind kind) {
        Domain domain = controllingDomain(entity);
        return domain != null && domain.domainKind() == kind && domain.domainSuppressesControl();
    }

    private static LevelState state(Level level) {
        return LEVELS.computeIfAbsent(level.dimension(), key -> new LevelState());
    }

    /**
     * Rebuilds the control map at most once per level and tick. Domains are visited in registration
     * order, so a domain opened later overwrites the earlier claim on the same entity (takeover).
     */
    private static LevelState sweep(ServerLevel level) {
        LevelState state = state(level);
        long now = level.getGameTime();
        if (state.tick == now) return state;
        state.tick = now;
        state.control.clear();
        // Purge here as well (not only in unregister) so an entity torn down by a path that skipped
        // remove() cannot leave a stale domain behind in ALL / BY_OWNER forever.
        for (int i = state.domains.size() - 1; i >= 0; i--) {
            Domain domain = state.domains.get(i);
            if (domain.domainEntity().isRemoved() || !domain.domainEntity().isAlive()) {
                state.domains.remove(i);
                ALL.remove(domain);
                LEVEL_OF.remove(domain);
                UUID owner = domain.domainOwnerId();
                if (owner != null) BY_OWNER.remove(owner, domain);
            }
        }
        for (Domain domain : state.domains) {
            // A closing domain gives control back immediately: AI, movement and interaction resume at
            // once instead of waiting for the visual close animation to finish.
            if (domain.domainClosing()) continue;
            domain.collectControlled(level, entity -> state.control.put(entity.getId(), domain));
        }
        return state;
    }
}
