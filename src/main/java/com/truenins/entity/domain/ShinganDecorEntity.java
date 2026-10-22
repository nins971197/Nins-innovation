package com.truenins.entity.domain;

import com.truenins.register.TNEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * Ground decoration inside 真赝相爱 (variant 0 = katana, 1 = white stone cross, 2 = red-white knot).
 * Immobile, no physics, no gravity; discarded by its parent domain when it closes.
 */
public class ShinganDecorEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_VARIANT =
        SynchedEntityData.defineId(ShinganDecorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_START =
        SynchedEntityData.defineId(ShinganDecorEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_END =
        SynchedEntityData.defineId(ShinganDecorEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Vector3f> DATA_LINK =
        SynchedEntityData.defineId(ShinganDecorEntity.class, EntityDataSerializers.VECTOR3);

    private int age;

    public ShinganDecorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ShinganDecorEntity(Level level, double x, double y, double z, int variant, float yaw) {
        this(TNEntities.SHINGAN_DECOR.get(), level);
        this.setPos(x, y, z);
        this.setYRot(yaw);
        this.setXRot(0.0F);
        setVariant(variant);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_VARIANT, 0);
        this.entityData.define(DATA_START, 0L);
        this.entityData.define(DATA_END, 0L);
        this.entityData.define(DATA_LINK, new Vector3f());
    }

    public void setLifetime(long start, long end) {
        this.entityData.set(DATA_START, start);
        this.entityData.set(DATA_END, end);
    }

    public void linkTo(ShinganDecorEntity other) {
        Vec3 offset = other.position().subtract(position());
        this.entityData.set(DATA_LINK, new Vector3f((float)offset.x, (float)offset.y, (float)offset.z));
    }

    public Vec3 ropeEnd() {
        Vector3f v = this.entityData.get(DATA_LINK);
        return new Vec3(v.x, v.y, v.z);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 end = position().add(ropeEnd());
        return new AABB(position(), end).inflate(variant() == 2 ? 2 : 6);
    }

    public float appearance(float partial) {
        long end = this.entityData.get(DATA_END);
        if (end == 0) return 1;
        double now = level().getGameTime() + partial;
        double enter = Math.min(1, Math.max(0, (now - this.entityData.get(DATA_START)) / 18.0));
        double leave = Math.min(1, Math.max(0, (end - now) / 25.0));
        return (float) (enter * enter * (3 - 2 * enter) * leave);
    }

    public float emergence(float partial) {
        if (this.entityData.get(DATA_END) == 0) return 1;
        float t = net.minecraft.util.Mth.clamp((float)(level().getGameTime() + partial
            - this.entityData.get(DATA_START)) / 30f, 0, 1);
        return t * t * (3 - 2 * t);
    }

    public int variant() {
        return this.entityData.get(DATA_VARIANT);
    }

    private void setVariant(int value) {
        this.entityData.set(DATA_VARIANT, value);
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;
        // Safety net in case the parent domain never cleaned us up.
        long end = this.entityData.get(DATA_END);
        if (!level().isClientSide && ((end == 0 && this.age > 20 * 120)
                || (end > 0 && level().getGameTime() > end))) {
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // noSave entity — nothing to restore
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // noSave entity — nothing to persist
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512.0D * 512.0D;
    }
}
