package com.truenins.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class TNOrbEntity extends Entity {

    protected static final int HOLD_TICKS = 8;

    protected static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_SCALE =
        net.minecraft.network.syncher.SynchedEntityData.defineId(
            TNOrbEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    protected static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_EXPLODE =
        net.minecraft.network.syncher.SynchedEntityData.defineId(
            TNOrbEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    protected int life;
    @Nullable
    protected UUID ownerId;

    protected TNOrbEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SCALE, 1.0F);
        this.entityData.define(DATA_EXPLODE, -1);
    }

    public float orbScale() {
        return this.entityData.get(DATA_SCALE);
    }

    protected void setOrbScale(float value) {
        this.entityData.set(DATA_SCALE, value);
    }

    public int explodeProgress() {
        return this.entityData.get(DATA_EXPLODE);
    }

    protected void setExplodeProgress(int value) {
        this.entityData.set(DATA_EXPLODE, value);
    }

    protected float growOver(int tick, int total) {
        float t = total <= 0 ? 1.0F : Math.min(1.0F, tick / (float) total);
        return 0.16F + 0.84F * (1.0F - (1.0F - t) * (1.0F - t));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.getInt("TNLife");
        if (tag.hasUUID("TNOwner")) this.ownerId = tag.getUUID("TNOwner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TNLife", this.life);
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
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {}

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0D * 96.0D;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    public void setOwner(@Nullable net.minecraft.world.entity.LivingEntity owner) {
        this.ownerId = owner == null ? null : owner.getUUID();
    }

    @Nullable
    public net.minecraft.world.entity.LivingEntity owner() {
        if (this.ownerId == null || this.level().isClientSide) return null;
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel server)) return null;
        net.minecraft.world.entity.Entity found = server.getEntity(this.ownerId);
        return found instanceof net.minecraft.world.entity.LivingEntity living ? living : null;
    }

    public int life() {
        return this.life;
    }

    public static Vec3 handPos(net.minecraft.world.entity.LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-4D) flat = new Vec3(0.0D, 0.0D, 1.0D);
        flat = flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x);
        return owner.getEyePosition()
            .add(flat.scale(0.72D))
            .add(side.scale(-0.34D))
            .add(0.0D, -0.18D, 0.0D);
    }

    protected void moveToPos(Vec3 pos) {
        this.setPos(pos.x, pos.y, pos.z);
    }
}
