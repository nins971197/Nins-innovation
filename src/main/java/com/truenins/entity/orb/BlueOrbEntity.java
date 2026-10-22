package com.truenins.entity.orb;

import com.truenins.TrueNinsConfig;
import com.truenins.register.TNEntities;
import com.truenins.register.TNParticles;
import com.truenins.register.TNSounds;
import com.truenins.spell.OrbDamage;
import com.truenins.spell.orb.OrbFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

public class BlueOrbEntity extends TNOrbEntity {

    public static final String SPELL_ID = "truenins:ao";
    public static final Map<UUID, BlueOrbEntity> CHARGING = new HashMap<>();
    public static final Set<UUID> FAILED_CHARGES = new HashSet<>();

    private static final DustParticleOptions PIXEL_DUST = new DustParticleOptions(new Vector3f(.12f, .42f, .95f), .55f);

    private static final int LAUNCH_TICKS = 8;
    private static final int CHARGE_TIMEOUT = 60;
    private static final int MAX_VISIBLE_SHARDS = 96;

    private final Map<UUID, Drag> eaten = new LinkedHashMap<>();
    private final Deque<BlockState> consumedBlocks = new ArrayDeque<>();
    private int slot;
    private int chargeTicks = 20;
    private boolean launched;
    private int launchLife;
    private double travelled;
    private Vec3 launchDir = new Vec3(0.0D, 0.0D, 1.0D);
    private Vec3 launchFrom = Vec3.ZERO;
    private Vec3 center = Vec3.ZERO;
    private Vec3 segFrom = Vec3.ZERO;
    private Vec3 lastConsumePos;
    private boolean drainsMana;

    private static final class Drag {
        double angle;
        double distance;
        final int slot;

        Drag(double angle, int slot, double distance) {
            this.angle = angle;
            this.slot = slot;
            this.distance = distance;
        }
    }

    public BlueOrbEntity(EntityType<? extends BlueOrbEntity> type, Level level) {
        super(type, level);
    }

    public BlueOrbEntity(Level level, LivingEntity owner, int chargeTicks, boolean drainsMana) {
        super(TNEntities.BLUE_ORB.get(), level);
        setOwner(owner);
        this.chargeTicks = Math.max(4, chargeTicks);
        this.drainsMana = drainsMana;
        this.center = owner.position();
        this.launchFrom = owner.position();
        this.setPos(owner.getX(), owner.getY() + 1.05D, owner.getZ());
        setOrbScale(0.20F);
    }

    public void launch() {
        if (this.launched) return;
        this.launched = true;
        this.launchLife = this.life;
        this.launchFrom = this.position();
        this.lastConsumePos = this.position();

        LivingEntity owner = owner();
        if (owner != null) {
            this.launchDir = OrbFlight.direction(owner.getLookAngle());
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
            this.center = owner.position();
        }
        setOrbScale(1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        this.life++;
        ServerLevel server = (ServerLevel) this.level();
        LivingEntity owner = owner();
        if (!this.launched && (owner == null || !owner.isAlive())) {
            this.discard();
            return;
        }
        this.segFrom = this.position();

        if (!this.launched) {
            if (this.life > this.chargeTicks + CHARGE_TIMEOUT) {
                if (this.ownerId != null) CHARGING.remove(this.ownerId, this);
                this.discard();
                return;
            }
            if (owner != null) {
                Vec3 look = owner.getLookAngle();
                moveToPos(owner.getEyePosition().add(look.scale(1.55D)));
                if (this.drainsMana && this.life % 10 == 0) {
                    if (!OrbDamage.drainMana(owner,
                            Math.max(1, TrueNinsConfig.aoChargeManaPerSecond() / 2))) {
                        if (this.ownerId != null) FAILED_CHARGES.add(this.ownerId);
                        this.discard();
                        return;
                    }
                }
            }
            setOrbScale(Math.min(1.0F, 0.20F + 0.80F * (this.life / (float) this.chargeTicks)));
        } else {
            int t = this.life - this.launchLife;
            if (t >= TrueNinsConfig.aoDurationTicks()) {
                dissipate(server);
                return;
            }
            if (t <= LAUNCH_TICKS) {
                double e = 1.0D - Math.pow(1.0D - t / (double) LAUNCH_TICKS, 2.4D);
                Vec3 next = this.launchFrom.add(this.launchDir.scale(Math.min(3.8D * e, TrueNinsConfig.aoMaxDistance())));
                this.travelled = next.distanceTo(this.launchFrom);
                moveToPos(next);
            } else if (this.travelled < TrueNinsConfig.aoMaxDistance()) {
                double speed = OrbFlight.step(TrueNinsConfig.aoFlightSpeed(), this.travelled, TrueNinsConfig.aoMaxDistance());
                this.travelled += speed;
                moveToPos(this.position().add(this.launchDir.scale(speed)));
            } else {
                moveToPos(this.launchFrom.add(this.launchDir.scale(this.travelled))
                    .add(0, Math.sin(this.life * 0.12D) * 0.02D, 0));
            }
        }

        pull(owner);
        dragEaten(server);
        consume(server);
        if (this.life % TrueNinsConfig.aoDamageIntervalTicks() == 0) damage(owner);

        if (this.life % 20 == 0) {
            server.playSound(null, getX(), getY(), getZ(),
                TNSounds.AO_HUM.get(), SoundSource.BLOCKS, 0.55F, 1.0F);
        }
    }

    private void pull(LivingEntity owner) {
        double base = this.launched ? 2.0D : TrueNinsConfig.aoChargeRadius();
        double radius = base + TrueNinsConfig.aoPullRadius();
        double strength = TrueNinsConfig.aoPullStrength();
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class,
            this.getBoundingBox().inflate(radius), e -> e != owner && e.isAlive() && !isProtected(e));

        for (LivingEntity target : targets) {
            Vec3 to = this.position().subtract(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
            double distance = to.length();
            if (distance < 0.05D || distance > radius + target.getBbWidth() * 0.5D) continue;

            double falloff = 1.0D - Math.min(1.0D, distance / radius) * 0.55D;
            Vec3 push = to.scale(strength * falloff / distance);
            Vec3 motion = target.getDeltaMovement();
            double lift = Math.max(0.0D, Math.min(0.24D,
                (this.getY() - (target.getY() + target.getBbHeight() * 0.5D)) * 0.13D));

            target.setDeltaMovement(
                motion.x * 0.82D + push.x,
                motion.y * 0.72D + push.y + lift,
                motion.z * 0.82D + push.z);
            target.fallDistance = 0.0F;
            target.hurtMarked = true;
        }
    }

    private void consume(ServerLevel server) {
        if (!TrueNinsConfig.aoConsumeBlocks()) return;
        if (this.life % TrueNinsConfig.aoConsumeIntervalTicks() != 0) return;

        int radius = TrueNinsConfig.aoConsumeRadius();
        double reach = radius + 0.5D;
        double reachSq = reach * reach;
        int cap = TrueNinsConfig.aoMaxShards();
        if (this.consumedBlocks.size() >= cap) return;

        Vec3 from = this.lastConsumePos == null ? this.segFrom : this.lastConsumePos;
        Vec3 to = this.position();
        this.lastConsumePos = to;
        Vec3 seg = to.subtract(from);
        double segLenSq = seg.lengthSqr();

        int minX = Mth.floor(Math.min(from.x, to.x) - reach);
        int maxX = Mth.floor(Math.max(from.x, to.x) + reach);
        int minY = Mth.floor(Math.min(from.y, to.y) - reach);
        int maxY = Mth.floor(Math.max(from.y, to.y) + reach);
        int minZ = Mth.floor(Math.min(from.z, to.z) - reach);
        int maxZ = Mth.floor(Math.max(from.z, to.z) + reach);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int taken = 0;
        int visibleSpawned = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    double px = x + 0.5D - from.x;
                    double py = y + 0.5D - from.y;
                    double pz = z + 0.5D - from.z;

                    double along = segLenSq < 1.0E-9D ? 0.0D
                        : Mth.clamp((px * seg.x + py * seg.y + pz * seg.z) / segLenSq, 0.0D, 1.0D);
                    double cx = px - seg.x * along;
                    double cy = py - seg.y * along;
                    double cz = pz - seg.z * along;
                    if (cx * cx + cy * cy + cz * cz > reachSq) continue;

                    if (!server.hasChunk(x >> 4, z >> 4)) continue;
                    BlockState state = server.getBlockState(cursor);
                    if (state.isAir()) continue;
                    if (!mayBreak(server, cursor, state)) continue;
                    if (!TrueNinsConfig.aoBreakContainers() && state.hasBlockEntity()) continue;
                    if (this.consumedBlocks.size() >= cap) continue;

                    if (!server.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2)) continue;
                    this.consumedBlocks.addLast(state);
                    taken++;

                    // Keep terrain processing independent of the visual entity budget.
                    if (visibleSpawned >= 12) continue;

                    if (this.eaten.size() >= Math.min(cap, MAX_VISIBLE_SHARDS)) {
                        java.util.Iterator<Map.Entry<UUID, Drag>> oldest = this.eaten.entrySet().iterator();
                        if (oldest.hasNext()) {
                            Entity drop = server.getEntity(oldest.next().getKey());
                            if (drop != null) drop.discard();
                            oldest.remove();
                        }
                    }

                    OrbShardEntity shard = new OrbShardEntity(server,
                        x + 0.5D, y + 0.5D, z + 0.5D, state);
                    if (!server.addFreshEntity(shard)) continue;
                    visibleSpawned++;
                    int index = this.slot++;
                    double sx = shard.getX() - getX();
                    double sz = shard.getZ() - getZ();
                    this.eaten.put(shard.getUUID(),
                        new Drag(Math.atan2(sz, sx), index, Math.max(0.5D, Math.sqrt(sx * sx + sz * sz))));

                }
            }
        }

        finishConsume(server, taken);
    }

    private void finishConsume(ServerLevel server, int taken) {
        if (taken > 0 && this.life % 6 == 0) {
            server.playSound(null, getX(), getY(), getZ(), TNSounds.BLOCK_BREAK.get(),
                SoundSource.BLOCKS, 0.55F, 0.85F + this.random.nextFloat() * 0.35F);
        }
    }

    private void dragEaten(ServerLevel server) {
        if (this.eaten.isEmpty()) return;
        int particleBudget = this.life % 4 == 0 ? 8 : 0;

        java.util.Iterator<Map.Entry<UUID, Drag>> it = this.eaten.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Drag> entry = it.next();
            Entity found = server.getEntity(entry.getKey());
            if (!(found instanceof OrbShardEntity shard)) {
                it.remove();
                continue;
            }

            Drag drag = entry.getValue();
            int index = drag.slot;
            double hold = 1.45D + 0.095D * (index % 7);
            drag.distance += (hold - drag.distance) * 0.16D;
            drag.angle += 0.10D + (index % 5) * 0.012D;
            double height = 0.28D * Math.sin(index * 0.7D);

            double sx = getX() + Math.cos(drag.angle) * drag.distance;
            double sz = getZ() + Math.sin(drag.angle) * drag.distance;
            shard.setPos(sx, getY() + height, sz);

            if (particleBudget > 0 && index % 5 == (this.life % 5) && drag.distance > hold + 0.12D) {
                particleBudget--;
                Vec3 pull = this.position().subtract(shard.position()).normalize().scale(0.42D);
                server.sendParticles(PIXEL_DUST,
                    sx, getY() + height, sz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                server.sendParticles(TNParticles.MOTE_STAR.get(),
                    sx, getY() + height, sz, 1, pull.x, pull.y, pull.z, 0.35D);
            }
        }
    }

    private void dissipate(ServerLevel server) {
        for (UUID id : this.eaten.keySet()) {
            Entity found = server.getEntity(id);
            if (found != null) found.discard();
        }
        releaseBlocks(server);
        this.eaten.clear();

        server.sendParticles(PIXEL_DUST, getX(), getY(), getZ(),
            14, 0.45D, 0.45D, 0.45D, 0.18D);
        server.playSound(null, getX(), getY(), getZ(),
            TNSounds.HE_EXPLODE.get(), SoundSource.BLOCKS, 0.45F, 1.7F);
        this.discard();
    }

    private void releaseBlocks(ServerLevel server) {
        BlockPos origin = BlockPos.containing(getX(), groundLevel(server), getZ());
        int index = 0;
        for (BlockState state : this.consumedBlocks) {
            if (state.isAir()) continue;

            double angle = index * 2.399963D;
            double spread = 0.26D * Math.sqrt(index);
            int layer = (int) Math.floor(spread / 0.85D);
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * spread);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * spread);
            place(server, new BlockPos(x, origin.getY() + layer, z), state);
            index++;
        }
        this.consumedBlocks.clear();
    }

    private int groundLevel(ServerLevel server) {
        BlockPos.MutableBlockPos cursor = this.blockPosition().mutable();
        for (int i = 0; i < 20; i++) {
            if (!server.getBlockState(cursor).isAir()) return cursor.getY() + 1;
            cursor.move(0, -1, 0);
        }
        return this.blockPosition().getY();
    }

    private static void place(ServerLevel server, BlockPos pos, BlockState state) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < 32 && cursor.getY() < server.getMaxBuildHeight(); i++) {
            BlockState at = server.getBlockState(cursor);
            if (at.isAir() || at.canBeReplaced()) {
                server.setBlock(cursor, state, 3);
                return;
            }
            cursor.move(0, 1, 0);
        }
    }

    private void damage(LivingEntity owner) {
        hitNearby(owner, 2.4D, TrueNinsConfig.aoDamageAmount(), SPELL_ID, null);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
            for (UUID id : this.eaten.keySet()) {
                Entity shard = server.getEntity(id);
                if (shard != null) shard.discard();
            }
            if (!this.consumedBlocks.isEmpty()) releaseBlocks(server);
            this.eaten.clear();
            if (!this.launched && this.ownerId != null) CHARGING.remove(this.ownerId, this);
        }
        super.remove(reason);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0D * 128.0D;
    }
}
