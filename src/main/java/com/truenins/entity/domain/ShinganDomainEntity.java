package com.truenins.entity.domain;

import com.truenins.TrueNinsConfig;
import com.truenins.compat.irons.ShinganMagicProjectiles;
import com.truenins.register.TNEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 真赝相爱 domain: an immobile barrier that scatters katana / crosses / knots on the ground,
 * then rains random vanilla projectiles down on the creatures inside (undead first) while it lasts.
 */
public class ShinganDomainEntity extends Entity implements DomainRegistry.Domain {

    public static final float DEFAULT_RADIUS = 24.0F;
    public static final int DEFAULT_DURATION_TICKS = 600;

    private static final EntityType<?>[] PROJECTILES = {
        EntityType.ARROW,
        EntityType.SPECTRAL_ARROW,
        EntityType.SMALL_FIREBALL,
        EntityType.FIREBALL,
        EntityType.WITHER_SKULL,
        EntityType.DRAGON_FIREBALL,
        EntityType.SHULKER_BULLET,
        EntityType.TRIDENT
    };

    private static final EntityDataAccessor<Float> DATA_RADIUS =
        SynchedEntityData.defineId(ShinganDomainEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_REMAINING =
        SynchedEntityData.defineId(ShinganDomainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_START =
        SynchedEntityData.defineId(ShinganDomainEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_END =
        SynchedEntityData.defineId(ShinganDomainEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_PULSE =
        SynchedEntityData.defineId(ShinganDomainEntity.class, EntityDataSerializers.LONG);

    private int age;
    private int durationTicks = DEFAULT_DURATION_TICKS;
    private int attackInterval = 30;
    private int nextAttackAge = 10;
    private int baseAttacks = 30;
    private int attacksFired;
    private final java.util.Set<UUID> countedEntities = new java.util.HashSet<>();
    @Nullable
    private UUID ownerId;
    private boolean decorationsSpawned;
    private long domainSeq;
    private boolean closing;
    private boolean registered;
    private final List<ShinganDecorEntity> decor = new ArrayList<>();

    public ShinganDomainEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.noCulling = true;
    }

    public ShinganDomainEntity(Level level, LivingEntity caster, int durationTicks, float radius, int attacks) {
        this(TNEntities.SHINGAN_DOMAIN.get(), level);
        setOwner(caster);
        this.durationTicks = Math.max(20, durationTicks);
        this.setPos(caster.getX(), caster.getY(), caster.getZ());
        this.setYRot(caster.getYRot());
        this.setXRot(caster.getXRot());
        this.setRadius(Math.max(1.0F, radius));
        this.setRemaining(this.durationTicks);
        this.entityData.set(DATA_START, level.getGameTime());
        this.entityData.set(DATA_END, level.getGameTime() + this.durationTicks + 30);
        this.attackInterval = Math.max(2, this.durationTicks / Math.max(1, attacks));
        this.baseAttacks = Math.max(1, attacks);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RADIUS, DEFAULT_RADIUS);
        this.entityData.define(DATA_REMAINING, DEFAULT_DURATION_TICKS);
        this.entityData.define(DATA_START, 0L);
        this.entityData.define(DATA_END, 0L);
        this.entityData.define(DATA_PULSE, -100L);
    }

    public float domainRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    private void setRadius(float value) {
        this.entityData.set(DATA_RADIUS, value);
    }

    public int remainingTicks() {
        return this.entityData.get(DATA_REMAINING);
    }

    public float visualAge(float partial) {
        return Math.max(0, level().getGameTime() - this.entityData.get(DATA_START) + partial);
    }

    public float appearance(float partial) {
        float enter = Math.min(1f, Math.max(0f, visualAge(partial) / 36f));
        enter = enter * enter * (3f - 2f * enter);
        float leave = Math.min(1f, Math.max(0f,
            (this.entityData.get(DATA_END) - level().getGameTime() - partial) / 30f));
        leave = leave * leave * (3f - 2f * leave);
        return enter * leave;
    }

    /** Radius transition used by the renderer: expand on entry and collapse during release. */
    public float transition(float partial) {
        float open = Math.min(1f, Math.max(0f, visualAge(partial) / 36f));
        open = open * open * (3f - 2f * open);
        float close = Math.min(1f, Math.max(0f,
            (this.entityData.get(DATA_END) - level().getGameTime() - partial) / 30f));
        close = close * close * (3f - 2f * close);
        return open * close;
    }

    public float pulseAge(float partial) {
        return level().getGameTime() - this.entityData.get(DATA_PULSE) + partial;
    }

    private void setRemaining(int value) {
        this.entityData.set(DATA_REMAINING, value);
    }

    @Nullable
    public LivingEntity owner() {
        if (this.ownerId == null) return null;
        if (!(this.level() instanceof ServerLevel server)) return null;
        Entity entity = server.getEntity(this.ownerId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void setOwner(LivingEntity caster) {
        this.ownerId = caster.getUUID();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        // Registration also closes any other domain this caster still had open.
        if (!this.registered) { this.registered = true; DomainRegistry.register(this); }
        this.age++;
        setRemaining(Math.max(0, this.durationTicks - this.age));

        if (!this.decorationsSpawned) {
            this.decorationsSpawned = true;
            spawnDecorations();
        }

        if (this.age <= this.durationTicks) {
            if (this.age == 1 || this.age % 5 == 0) countLivingInside();
            int remaining = this.baseAttacks + this.countedEntities.size() - this.attacksFired;
            if (remaining > 0 && this.age >= this.nextAttackAge && this.age >= 10) {
                if (attackTick()) this.attacksFired++;
                remaining = this.baseAttacks + this.countedEntities.size() - this.attacksFired;
                this.attackInterval = Math.max(1, (int) Math.floor(
                    ((this.durationTicks - this.age) / (double) Math.max(1, remaining))
                        * TrueNinsConfig.shinganAttackIntervalScale()));
                this.nextAttackAge = this.age + this.attackInterval;
            }
        }

        if (this.age > this.durationTicks + 30) closeDomain("expired");
    }

    private void countLivingInside() {
        AABB box = new AABB(position(), position()).inflate(domainRadius());
        LivingEntity caster = owner();
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != caster && e.distanceToSqr(position()) <= domainRadius() * domainRadius())) {
            if (this.countedEntities.add(entity.getUUID())) {
                int remaining = this.baseAttacks + this.countedEntities.size() - this.attacksFired;
                this.nextAttackAge = Math.min(this.nextAttackAge,
                    this.age + Math.max(1, (int) Math.floor(
                        ((this.durationTicks - this.age) / (double) Math.max(1, remaining))
                            * TrueNinsConfig.shinganAttackIntervalScale())));
            }
        }
    }

    public int attacksFired() { return this.attacksFired; }
    public int attackBudget() { return this.baseAttacks + this.countedEntities.size(); }

    private void spawnDecorations() {
        ServerLevel level = (ServerLevel) this.level();
        int count = TrueNinsConfig.shinganDecorCount();
        float radius = domainRadius();
        RandomSource rnd = level.random;
        Vec3 center = this.position();
        LivingEntity caster = owner();
        for (int i = 0; i < count; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2.0D;
            double dist = Math.sqrt(rnd.nextDouble()) * (double) radius * 0.92D;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            if (caster != null && radius > 6 && Math.hypot(x - caster.getX(), z - caster.getZ()) < 2.6) {
                // Reserve a readable casting space without reducing the configured prop count.
                angle += Math.PI;
                x = center.x + Math.cos(angle) * Math.max(4, dist);
                z = center.z + Math.sin(angle) * Math.max(4, dist);
            }
            int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
            // Decorations must never generate chunks or jump to a cave's roof.
            if (!level.hasChunk(bx >> 4, bz >> 4)) continue;
            double groundY = decorationGround(level, bx, bz);
            // Cosmetic distribution: keep swords readable between the large stone monuments.
            float roll = rnd.nextFloat();
            int variant = roll < 0.44F ? 1 : roll < 0.92F ? 0 : 2;
            double y = (double) groundY + 0.03D;
            float tilt;
            if (variant == 2) {
                // knots hover in the air, unsupported
                y = (double) groundY + 6.0D + rnd.nextDouble() * 3.0D;
                tilt = (rnd.nextFloat() - 0.5F) * 40.0F;
            } else if (variant == 0) {
                // katanas lean at random angles
                tilt = (rnd.nextFloat() - 0.5F) * 48.0F;
            } else {
                // crosses lean slightly
                tilt = (rnd.nextFloat() - 0.5F) * 70.0F;
            }
            ShinganDecorEntity d = new ShinganDecorEntity(level, x, y, z, variant, rnd.nextFloat() * 360.0F);
            d.setXRot(tilt);
            d.setLifetime(level.getGameTime() + 5 + (long) (dist * .85) + rnd.nextInt(9),
                this.entityData.get(DATA_START) + this.durationTicks + 30);
            level.addFreshEntity(d);
            this.decor.add(d);
        }
        List<ShinganDecorEntity> knots = new ArrayList<>();
        for (ShinganDecorEntity d : this.decor) if (d.variant() == 2) knots.add(d);
        knots.sort(java.util.Comparator.comparingDouble(d -> Math.atan2(d.getZ() - getZ(), d.getX() - getX())));
        if (knots.size() > 1) for (int i = 0; i < knots.size(); i++) {
            knots.get(i).linkTo(knots.get((i + 1) % knots.size()));
        }
    }

    private double decorationGround(ServerLevel level, int x, int z) {
        int top = Math.min(level.getMaxBuildHeight() - 2, (int) Math.floor(getY()) + 4);
        int bottom = Math.max(level.getMinBuildHeight(), top - 28);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = top; y >= bottom; y--) {
            p.set(x, y, z);
            var state = level.getBlockState(p);
            if (state.is(net.minecraft.tags.BlockTags.LEAVES) || state.is(net.minecraft.tags.BlockTags.LOGS)) continue;
            var shape = state.getCollisionShape(level, p);
            if (!shape.isEmpty() && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) {
                return y + shape.max(net.minecraft.core.Direction.Axis.Y);
            }
        }
        return getY();
    }

    private boolean attackTick() {
        ServerLevel level = (ServerLevel) this.level();
        float radius = domainRadius();
        double r2 = (double) radius * radius;
        Vec3 center = this.position();
        AABB box = new AABB(center, center).inflate(radius);
        LivingEntity caster = owner();
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != caster);
        found.removeIf(e -> e.distanceToSqr(center) > r2);
        if (found.isEmpty()) return false;

        List<LivingEntity> undead = new ArrayList<>();
        for (LivingEntity e : found) {
            if (e.getMobType() == MobType.UNDEAD) undead.add(e);
        }
        List<LivingEntity> pool = undead.isEmpty() ? found : undead;
        LivingEntity target = pool.get(level.random.nextInt(pool.size()));
        this.entityData.set(DATA_PULSE, level.getGameTime());
        summonProjectile(level, target, caster);
        return true;
    }

    private void summonProjectile(ServerLevel level, LivingEntity target, LivingEntity caster) {
        if (level.random.nextBoolean() || !ModList.get().isLoaded("irons_spellbooks")) {
            summonVanillaProjectile(level, target, caster);
        } else {
            ShinganMagicProjectiles.summon(level, this.position(), target, caster);
        }
    }

    private void summonVanillaProjectile(ServerLevel level, LivingEntity target, LivingEntity caster) {
        EntityType<?> type = PROJECTILES[level.random.nextInt(PROJECTILES.length)];
        Entity e = type.create(level);
        if (e == null) return;

        Vec3 from = this.position().add(
            (level.random.nextDouble() - 0.5D) * 6.0D,
            6.0D + level.random.nextDouble() * 6.0D,
            (level.random.nextDouble() - 0.5D) * 6.0D);
        e.setPos(from.x, from.y, from.z);

        Vec3 dir = target.getEyePosition().subtract(from).normalize();
        float speed = TrueNinsConfig.shinganAttackProjectileSpeed()
            * (0.82F + level.random.nextFloat() * 0.36F);

        if (e instanceof Projectile p) {
            p.shoot(dir.x, dir.y, dir.z, speed, 1.5F);
        }
        if (e instanceof AbstractHurtingProjectile h) {
            h.setOwner(caster);
        } else if (e instanceof AbstractArrow a) {
            a.setOwner(caster);
            a.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        } else if (e instanceof ThrowableProjectile t) {
            t.setOwner(caster);
        } else if (e instanceof ShulkerBullet s) {
            s.setOwner(caster);
        }

        level.addFreshEntity(e);
    }

    // ---------------------------------------------------------------- DomainRegistry.Domain
    @Override public DomainRegistry.Kind domainKind() { return DomainRegistry.Kind.SHINGAN; }
    @Override public java.util.UUID domainOwnerId() { return ownerId; }
    @Override public Entity domainEntity() { return this; }
    @Override public long domainSeq() { return domainSeq; }
    @Override public void setDomainSeq(long seq) { domainSeq = seq; }
    /** This domain only rains projectiles; it deliberately does not freeze AI or movement. */
    @Override public boolean domainSuppressesControl() { return false; }
    @Override public boolean domainClosing() { return closing; }
    /**
     * Never claims control. This domain only rains projectiles; if it took control it would silently
     * release the AI / movement freeze of an overlapping void or judgment domain, because control is
     * exclusive and this kind suppresses nothing. Registration is still needed for the caster-level
     * mutual exclusion, which is handled by the registry's owner table.
     */
    @Override public boolean controlsEntity(LivingEntity e) { return false; }
    /** No control claim, so no per-tick AABB query either. */
    @Override public void collectControlled(ServerLevel level, Consumer<LivingEntity> out) { }
    @Override public void closeDomain(String reason) {
        if (closing) return;
        closing = true;
        DomainRegistry.unregister(this);
        if (!this.level().isClientSide) this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        closing = true;
        DomainRegistry.unregister(this);
        if (!this.level().isClientSide) {
            for (ShinganDecorEntity d : this.decor) {
                if (d.isAlive()) d.discard();
            }
            this.decor.clear();
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("TNAge");
        this.durationTicks = tag.getInt("TNDuration");
        this.attackInterval = tag.getInt("TNAttackInterval");
        this.baseAttacks = tag.contains("TNBaseAttacks") ? Math.max(1, tag.getInt("TNBaseAttacks")) : 30;
        this.attacksFired = tag.getInt("TNAttacksFired");
        this.nextAttackAge = Math.max(10, this.age + 1);
        this.decorationsSpawned = tag.getBoolean("TNDecor");
        if (tag.hasUUID("TNOwner")) this.ownerId = tag.getUUID("TNOwner");
        setRadius(tag.contains("TNRadius") ? tag.getFloat("TNRadius") : DEFAULT_RADIUS);
        this.entityData.set(DATA_START, level().getGameTime() - this.age);
        this.entityData.set(DATA_END, level().getGameTime() + this.durationTicks + 30 - this.age);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TNAge", this.age);
        tag.putInt("TNDuration", this.durationTicks);
        tag.putInt("TNAttackInterval", this.attackInterval);
        tag.putInt("TNBaseAttacks", this.baseAttacks);
        tag.putInt("TNAttacksFired", this.attacksFired);
        tag.putBoolean("TNDecor", this.decorationsSpawned);
        tag.putFloat("TNRadius", domainRadius());
        if (this.ownerId != null) tag.putUUID("TNOwner", this.ownerId);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512.0D * 512.0D;
    }

    @Override
    public boolean isAlwaysTicking() {
        return true;
    }
}
