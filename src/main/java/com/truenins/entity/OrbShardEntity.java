package com.truenins.entity;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class OrbShardEntity extends Entity {

    private static final EntityDataAccessor<BlockState> DATA_BLOCK =
        SynchedEntityData.defineId(OrbShardEntity.class, EntityDataSerializers.BLOCK_STATE);

    public OrbShardEntity(EntityType<? extends OrbShardEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public OrbShardEntity(Level level, double x, double y, double z, BlockState state) {
        this(TNEntities.ORB_SHARD.get(), level);
        setBlockState(state);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_BLOCK, Blocks.AIR.defaultBlockState());
    }

    public BlockState blockState() {
        return this.entityData.get(DATA_BLOCK);
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(DATA_BLOCK, state);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) this.setDeltaMovement(Vec3.ZERO);
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
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {}

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0D * 96.0D;
    }
}
