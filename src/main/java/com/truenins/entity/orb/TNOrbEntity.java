package com.truenins.entity.orb;

import com.truenins.spell.OrbDamage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class TNOrbEntity extends Entity {

    public static final int EXPLODE_TICKS = 18;
    protected static final int HOLD_TICKS = 8;

    protected static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_SCALE =
        net.minecraft.network.syncher.SynchedEntityData.defineId(
            TNOrbEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    protected static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_EXPLODE =
        net.minecraft.network.syncher.SynchedEntityData.defineId(
            TNOrbEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    protected int life;
    private double visualX, visualY, visualZ;
    private int interpolationSteps;
    private float previousVisualScale = 0.16F;
    private float currentVisualScale = 0.16F;
    @Nullable
    protected UUID ownerId;

    protected TNOrbEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        if (teleport) {
            super.lerpTo(x, y, z, yaw, pitch, steps, true);
            this.interpolationSteps = 0;
            return;
        }
        this.visualX = x;
        this.visualY = y;
        this.visualZ = z;
        this.interpolationSteps = Math.max(1, Math.min(3, steps));
        this.setYRot(yaw);
        this.setXRot(pitch);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) return;
        if (this.interpolationSteps > 0) {
            this.setPos(getX() + (this.visualX - getX()) / this.interpolationSteps,
                getY() + (this.visualY - getY()) / this.interpolationSteps,
                getZ() + (this.visualZ - getZ()) / this.interpolationSteps);
            this.interpolationSteps--;
        }
        this.previousVisualScale = this.currentVisualScale;
        this.currentVisualScale += (orbScale() - this.currentVisualScale) * 0.55F;
    }

    public float visualScale(float partialTick) {
        return net.minecraft.util.Mth.lerp(partialTick, this.previousVisualScale, this.currentVisualScale);
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

    protected static boolean isProtected(LivingEntity entity) {
        return entity instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    protected static boolean mayBreak(net.minecraft.server.level.ServerLevel level,
                                      net.minecraft.core.BlockPos pos,
                                      net.minecraft.world.level.block.state.BlockState state) {
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) return false;
        return state.getDestroySpeed(level, pos) >= 0.0F;
    }

    protected void hitNearby(LivingEntity owner, double radius, float amount, String spellId,
                             @Nullable Set<UUID> once) {
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class,
            this.getBoundingBox().inflate(radius), e -> e != owner && e.isAlive() && !isProtected(e));
        for (LivingEntity target : targets) {
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            double reach = radius + target.getBbWidth() * 0.5D;
            if (this.position().distanceToSqr(center) > reach * reach) continue;
            if (once != null && !once.add(target.getUUID())) continue;
            OrbDamage.apply(target, this, owner, amount, spellId);
        }
    }
}
