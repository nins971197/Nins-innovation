package com.truenins.compat.irons;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsMod;
import com.truenins.register.TNMobEffects;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MadeInHeavenSpell extends AbstractSpell {

    /** Hard cap on the accelerated volume, independent of the config: 17^3 blocks at most, never 33^3. */
    private static final int MAX_RADIUS = 8;

    /** Per-tick work budget for the block pass; the remainder is deferred to the next tick by a cursor. */
    private static final int BLOCK_BUDGET = 1024;

    /** Per-tick work budget for the extra entity ticks. */
    private static final int ENTITY_BUDGET = 64;

    /** Time packets are only useful a few times per second; vanilla keeps its own schedule too. */
    private static final int TIME_SYNC_INTERVAL = 10;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Baseline> BASELINES = new HashMap<>();
    private static final Map<Integer, List<BlockPos>> SPHERE_CACHE = new HashMap<>();

    /**
     * World clock of a level before the first Made in Heaven session started there. Without it the
     * acceleration could only be added: nothing remembered the original time, so the spell could never
     * be undone and the world stayed permanently fast-forwarded.
     */
    private static final class Baseline {
        final ServerLevel level;
        final long startGameTime;
        final long startDayTime;
        int active;

        Baseline(ServerLevel level) {
            this.level = level;
            this.startGameTime = level.getGameTime();
            this.startDayTime = level.getDayTime();
        }
    }

    /** Per cast state: which level clock it drives and how far the block pass has progressed. */
    private static final class Session {
        final ServerLevel level;
        final Baseline baseline;
        int blockCursor;
        long lastSyncGameTime = -TIME_SYNC_INTERVAL;
        long lastSyncedDayTime = Long.MIN_VALUE;

        Session(ServerLevel level, Baseline baseline) {
            this.level = level;
            this.baseline = baseline;
        }
    }

    private final DefaultConfig defaultConfig = new DefaultConfig()
        .setMinRarity(SpellRarity.LEGENDARY)
        .setSchoolResource(TNIronsCompat.CURSE_ID)
        .setMaxLevel(1)
        .setCooldownSeconds(120.0D)
        .setAllowCrafting(false)
        .build();

    public MadeInHeavenSpell() {
        this.castTime = TrueNinsConfig.madeInHeavenDurationTicks();
    }

    @Override
    public ResourceLocation getSpellResource() {
        return new ResourceLocation(TrueNinsMod.MODID, "made_in_heaven");
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public SchoolType getSchoolType() {
        return TNIronsCompat.CURSE.get();
    }

    @Override
    public int getManaCost(int spellLevel) {
        int perSecond = TrueNinsConfig.madeInHeavenManaPerSecond();
        if (perSecond <= 0) return 0;
        return Math.max(1, perSecond / 2);
    }

    @Override
    public int getCastTime(int spellLevel) {
        return TrueNinsConfig.madeInHeavenDurationTicks();
    }

    @Override
    public int getSpellCooldown() {
        return TrueNinsConfig.madeInHeavenCooldownSeconds() * 20;
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public SpellRarity getRarity(int spellLevel) {
        return SpellRarity.LEGENDARY;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData magicData) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        // Recount from a clean slate so a recast can never stack two accelerations on one world.
        finish(player.getUUID(), "recast");
        Baseline baseline = BASELINES.get(serverLevel.dimension());
        if (baseline == null) {
            baseline = new Baseline(serverLevel);
            BASELINES.put(serverLevel.dimension(), baseline);
        }
        baseline.active++;
        SESSIONS.put(player.getUUID(), new Session(serverLevel, baseline));
        player.addEffect(new MobEffectInstance(TNMobEffects.MADE_IN_HEAVEN.get(), 24, 0, false, false, false));
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData magicData) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        // The switch now gates the time acceleration as well. Before, only the world tick part was
        // gated, so turning "accelerate world" off still fast-forwarded the clock.
        if (!TrueNinsConfig.madeInHeavenAccelerateWorld()) {
            finish(player.getUUID(), "switch-off");
            return;
        }

        // A dead caster can still be ticked by Iron for a moment; without this guard the safety net
        // would close the session and this method would immediately open a new one, every tick.
        if (!player.isAlive()) {
            finish(player.getUUID(), "caster-dead");
            return;
        }

        Session session = SESSIONS.get(player.getUUID());
        if (session != null && session.level != serverLevel) {
            // Dimension change: the old level's baseline has to be closed explicitly, otherwise its
            // active counter never drops and that level's clock is never rewound again.
            finish(player.getUUID(), "dimension-change");
            session = null;
        }
        if (session == null) {
            // Defensive: a cast restored from a save has no session yet.
            Baseline baseline = BASELINES.get(serverLevel.dimension());
            if (baseline == null) {
                baseline = new Baseline(serverLevel);
                BASELINES.put(serverLevel.dimension(), baseline);
            }
            baseline.active++;
            session = new Session(serverLevel, baseline);
            SESSIONS.put(player.getUUID(), session);
        }

        long step = Math.max(1L, TrueNinsConfig.madeInHeavenTimeTicksPerSecond() / 20L);
        serverLevel.setDayTime(serverLevel.getDayTime() + step);
        syncTime(session, serverLevel, false);

        accelerateNearby(session, serverLevel, player);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData magicData, boolean cancelled) {
        if (entity instanceof ServerPlayer player) finish(player.getUUID(), cancelled ? "cancelled" : "complete");
    }

    /** Restores the world clock of every running session; used when the server stops. */
    public static void restoreAll(String reason) {
        for (UUID id : new ArrayList<>(SESSIONS.keySet())) finish(id, reason);
    }

    /**
     * Safety net run once per server tick: a caster that dies, logs out or changes dimension must not
     * leave a permanently accelerated clock behind when Iron never reports the cast as finished.
     */
    public static void tickSessions(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        for (UUID id : new ArrayList<>(SESSIONS.keySet())) {
            Session session = SESSIONS.get(id);
            if (session == null) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || !player.isAlive() || player.serverLevel() != session.level) finish(id, "caster-gone");
        }
    }

    /**
     * Ends one session. The artificial part of the jump is rewound once the last session on that level
     * ends, keeping the time that would have passed naturally anyway.
     */
    private static void finish(UUID casterId, String reason) {
        Session session = SESSIONS.remove(casterId);
        if (session == null) return;
        Baseline baseline = session.baseline;
        baseline.active--;
        if (baseline.active > 0) return;
        ServerLevel level = baseline.level;
        long naturalElapsed = Math.max(0L, level.getGameTime() - baseline.startGameTime);
        level.setDayTime(baseline.startDayTime + naturalElapsed);
        syncTime(session, level, true);
        BASELINES.remove(level.dimension(), baseline);
    }

    /**
     * Sends the time packet only when the time actually moved, at most once per {@link #TIME_SYNC_INTERVAL}
     * ticks, and only to the level that changed. It used to allocate and broadcast a packet to every
     * player of the dimension on every single tick.
     */
    private static void syncTime(Session session, ServerLevel level, boolean force) {
        long gameTime = level.getGameTime();
        long dayTime = level.getDayTime();
        if (!force) {
            if (dayTime == session.lastSyncedDayTime) return;
            if (gameTime - session.lastSyncGameTime < TIME_SYNC_INTERVAL) return;
        }
        session.lastSyncGameTime = gameTime;
        session.lastSyncedDayTime = dayTime;
        ClientboundSetTimePacket packet = new ClientboundSetTimePacket(gameTime, dayTime,
            level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT));
        for (ServerPlayer viewer : level.players()) viewer.connection.send(packet);
    }

    private static void accelerateNearby(Session session, ServerLevel level, ServerPlayer caster) {
        int radius = Math.min(MAX_RADIUS, Math.max(0, TrueNinsConfig.madeInHeavenRadius()));
        if (radius <= 0) return;

        List<Entity> entities = level.getEntities(caster,
            caster.getBoundingBox().inflate(radius + 1.0D),
            e -> !(e instanceof Player) && !e.isPassenger() && e.isAlive());
        int moved = 0;
        for (Entity mover : entities) {
            if (moved >= ENTITY_BUDGET) break;
            if (mover.isRemoved()) continue;
            level.tickNonPassenger(mover);
            moved++;
        }

        // Cached sphere offsets plus a rotating cursor: bounded work per tick instead of a full 17^3 sweep.
        List<BlockPos> offsets = sphereOffsets(radius);
        int size = offsets.size();
        if (size == 0) return;
        int budget = Math.min(BLOCK_BUDGET, size);
        BlockPos center = caster.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < budget; i++) {
            BlockPos offset = offsets.get((session.blockCursor + i) % size);
            cursor.set(center.getX() + offset.getX(), center.getY() + offset.getY(), center.getZ() + offset.getZ());
            tickBlock(level, cursor);
        }
        session.blockCursor = (session.blockCursor + budget) % size;
    }

    private static List<BlockPos> sphereOffsets(int radius) {
        return SPHERE_CACHE.computeIfAbsent(radius, r -> {
            List<BlockPos> offsets = new ArrayList<>();
            int radiusSqr = r * r;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dy * dy + dz * dz > radiusSqr) continue;
                        offsets.add(new BlockPos(dx, dy, dz));
                    }
                }
            }
            return offsets;
        });
    }

    @SuppressWarnings("unchecked")
    private static void tickBlock(ServerLevel level, BlockPos.MutableBlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isRandomlyTicking()) {
            state.randomTick(level, pos, level.random);
            state = level.getBlockState(pos);
        }
        if (!state.hasBlockEntity()) return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || blockEntity.isRemoved()) return;

        BlockEntityTicker<BlockEntity> ticker = state.getTicker(level,
            (BlockEntityType<BlockEntity>) blockEntity.getType());
        if (ticker != null) ticker.tick(level, pos, state, blockEntity);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
            Component.translatable("ui.truenins.made_in_heaven.mana", TrueNinsConfig.madeInHeavenManaPerSecond()),
            Component.translatable("ui.truenins.made_in_heaven.duration", TrueNinsConfig.madeInHeavenDurationTicks() / 20),
            Component.translatable("ui.truenins.made_in_heaven.radius", TrueNinsConfig.madeInHeavenRadius()));
    }
}
