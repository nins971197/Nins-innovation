package com.truenins.entity.domain;

import com.truenins.register.TNEntities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;

public class VoidDomainEntity extends Entity implements DomainRegistry.Domain {

    public static final float RADIUS = 40.0F;
    public static final int DEFAULT_DURATION_TICKS = 600;
    private static final EntityDataAccessor<Float> DATA_RADIUS =
        SynchedEntityData.defineId(VoidDomainEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_REMAINING =
        SynchedEntityData.defineId(VoidDomainEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<java.util.Optional<UUID>> DATA_OWNER =
        SynchedEntityData.defineId(VoidDomainEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public boolean affects(LivingEntity target) {
        return isAlive() && remainingTicks() > 0
            && !this.entityData.get(DATA_OWNER).filter(target.getUUID()::equals).isPresent()
            && target.distanceToSqr(position()) <= (double)domainRadius() * domainRadius();
    }

    private int age;
    private int durationTicks = DEFAULT_DURATION_TICKS;
    @Nullable
    private UUID ownerId;
    private long domainSeq;
    private boolean closing;
    private boolean registered;

    public VoidDomainEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public VoidDomainEntity(Level level, LivingEntity caster, int durationTicks, float radius) {
        this(TNEntities.VOID_DOMAIN.get(), level);
        setOwner(caster);
        this.durationTicks = Math.max(20, durationTicks);
        this.setPos(caster.getX(), caster.getY(), caster.getZ());
        this.setYRot(caster.getYRot());
        this.setXRot(caster.getXRot());
        this.setRadius(Math.max(1.0F, radius));
        this.setRemaining(this.durationTicks);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RADIUS, RADIUS);
        this.entityData.define(DATA_OWNER, java.util.Optional.empty());
        this.entityData.define(DATA_REMAINING, DEFAULT_DURATION_TICKS);
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

    private void setRemaining(int value) {
        this.entityData.set(DATA_REMAINING, value);
    }

    public Vec3 look() {
        return this.getLookAngle();
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
        this.entityData.set(DATA_OWNER, java.util.Optional.of(this.ownerId));
    }

    // ---------------------------------------------------------------- DomainRegistry.Domain

    @Override
    public DomainRegistry.Kind domainKind() {
        return DomainRegistry.Kind.VOID;
    }

    @Override
    @Nullable
    public UUID domainOwnerId() {
        return this.ownerId;
    }

    @Override
    public Entity domainEntity() {
        return this;
    }

    @Override
    public long domainSeq() {
        return this.domainSeq;
    }

    @Override
    public void setDomainSeq(long seq) {
        this.domainSeq = seq;
    }

    @Override
    public boolean domainSuppressesControl() {
        return true;
    }

    /**
     * Control eligibility is kept exactly as the old VoidControl query: players and hostile mobs only.
     * The check is a statement about one entity, the actual set is collected once per tick in
     * {@link #collectControlled(ServerLevel, Consumer)} so no entity has to run its own 128 block query.
     */
    @Override
    public boolean controlsEntity(LivingEntity entity) {
        if (!entity.isAlive() || closing) return false;
        if (!(entity instanceof Player) && !(entity instanceof Enemy)) return false;
        if (entity instanceof Player player && player.isSpectator()) return false;
        return affects(entity);
    }

    @Override
    public void collectControlled(ServerLevel level, Consumer<LivingEntity> out) {
        if (closing) return;
        Vec3 center = position();
        double radius = domainRadius();
        double r2 = radius * radius;
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.distanceToSqr(center) <= r2)) {
            if (controlsEntity(entity)) out.accept(entity);
        }
    }

    /** Full teardown; the -10% health cut deliberately survives, it is removed by death only. */
    @Override
    public void closeDomain(String reason) {
        if (this.closing) return;
        this.closing = true;
        DomainRegistry.unregister(this);
        if (!this.level().isClientSide) this.discard();
    }

    @Override
    public boolean domainClosing() {
        return this.closing;
    }

    private void ensureRegistered() {
        if (this.registered) return;
        this.registered = true;
        DomainRegistry.register(this);
    }

    // ---------------------------------------------------------------- tick / lifecycle

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        // Registration also enforces mutual exclusion for this caster (one domain at a time).
        ensureRegistered();
        this.age++;
        setRemaining(Math.max(0, this.durationTicks - this.age));

        if (this.age > this.durationTicks + 30) closeDomain("expired");
        if (!this.closing && this.age <= this.durationTicks && this.age % 10 == 0) applyMaxHealthField();
    }

    private void applyMaxHealthField() {
        Vec3 center = this.position();
        double r2 = (double) domainRadius() * domainRadius();
        LivingEntity caster = owner();
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(domainRadius()), e -> e.isAlive() && e != caster
                    && e.distanceToSqr(center) <= r2)) {
            // Players keep the cut across relog / dimension change / restart, mobs keep it for the session.
            VoidHealthPenalty.apply(target);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        this.closing = true;
        DomainRegistry.unregister(this);
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("TNAge");
        this.durationTicks = tag.getInt("TNDuration");
        if (tag.hasUUID("TNOwner")) {
            this.ownerId = tag.getUUID("TNOwner");
            this.entityData.set(DATA_OWNER, java.util.Optional.of(this.ownerId));
        }
        setRadius(tag.contains("TNRadius") ? tag.getFloat("TNRadius") : RADIUS);
        setRemaining(Math.max(0, this.durationTicks - this.age));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TNAge", this.age);
        tag.putInt("TNDuration", this.durationTicks);
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
