package com.truenins.entity;

import com.truenins.TrueNinsConfig;
import com.truenins.register.TNEntities;
import com.truenins.register.TNParticles;
import com.truenins.register.TNSounds;
import com.truenins.spell.OrbDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlueOrbEntity extends TNOrbEntity {

    public static final String SPELL_ID = "truenins:ao";
    private static final int LAUNCH_TICKS = 9;

    private Vec3 launchDir = new Vec3(0.0D, 0.0D, 1.0D);
    private Vec3 launchFrom = Vec3.ZERO;
    private Vec3 center = Vec3.ZERO;
    private final Map<UUID, Shard> eaten = new HashMap<>();
    private int slot;
    private double ribbon;

    private record Shard(double angle, int slot) {}

    public BlueOrbEntity(EntityType<? extends BlueOrbEntity> type, Level level) {
        super(type, level);
    }

    public BlueOrbEntity(Level level, LivingEntity owner) {
        super(TNEntities.BLUE_ORB.get(), level);
        setOwner(owner);
        Vec3 look = owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        this.launchDir = flat.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : flat.normalize();
        this.launchFrom = handPos(owner);
        this.center = owner.position();
        this.setPos(this.launchFrom.x, this.launchFrom.y, this.launchFrom.z);
        setOrbScale(0.16F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        this.life++;
        ServerLevel server = (ServerLevel) this.level();

        if (this.life >= TrueNinsConfig.aoDurationTicks()) {
            release(server);
            server.sendParticles(TNParticles.MOTE_TIDE.get(), getX(), getY(), getZ(),
                14, 0.45D, 0.45D, 0.45D, 0.18D);
            server.playSound(null, getX(), getY(), getZ(),
                TNSounds.HE_EXPLODE.get(), SoundSource.BLOCKS, 0.45F, 1.7F);
            this.discard();
            return;
        }

        LivingEntity owner = owner();

        if (this.life <= HOLD_TICKS) {
            setOrbScale(growOver(this.life, HOLD_TICKS));
            if (owner != null) {
                this.launchFrom = handPos(owner);
                moveToPos(this.launchFrom);
            }
        } else {
            setOrbScale(1.0F);
            if (this.life <= HOLD_TICKS + LAUNCH_TICKS) {
                double t = (this.life - HOLD_TICKS) / (double) LAUNCH_TICKS;
                double eased = 1.0D - Math.pow(1.0D - t, 2.4D);
                moveToPos(this.launchFrom.add(this.launchDir.scale(3.8D * eased)));
            } else {
                if (owner != null) this.center = owner.position();
                double radius = TrueNinsConfig.aoOrbitRadius();
                double angle = (this.life - HOLD_TICKS - LAUNCH_TICKS) * TrueNinsConfig.aoOrbitSpeed();
                moveToPos(new Vec3(
                    this.center.x + Math.cos(angle) * radius,
                    this.center.y + 1.35D + Math.sin(angle * 2.0D) * 0.35D,
                    this.center.z + Math.sin(angle) * radius));
            }
        }

        if (this.life > HOLD_TICKS + LAUNCH_TICKS) {
            pull(owner);
            dragEaten();
            consume(server);
            if (this.life % TrueNinsConfig.aoDamageIntervalTicks() == 0) damage(owner);
        }

        intake(server);
        if (this.life % 20 == 0) {
            server.playSound(null, getX(), getY(), getZ(),
                TNSounds.AO_HUM.get(), SoundSource.BLOCKS, 0.55F, 1.0F);
        }
    }

    private void pull(LivingEntity owner) {
        double radius = TrueNinsConfig.aoOrbitRadius() + TrueNinsConfig.aoPullRadius();
        double strength = TrueNinsConfig.aoPullStrength();
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class,
            this.getBoundingBox().inflate(radius), e -> e != owner && e.isAlive() && !isProtected(e));

        for (LivingEntity target : targets) {
            Vec3 to = this.position().subtract(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
            double distance = to.length();
            if (distance < 0.05D) continue;

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

        int budget = TrueNinsConfig.aoConsumePerInterval();
        int radius = TrueNinsConfig.aoConsumeRadius();
        BlockPos center = this.blockPosition();
        int taken = 0;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-radius, -radius, -radius),
            center.offset(radius, radius, radius))) {

            if (taken >= budget) break;
            BlockState state = server.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getDestroySpeed(server, pos) < 0.0F) continue;
            if (state.hasBlockEntity()) continue;

            boolean shard = this.eaten.size() < TrueNinsConfig.aoMaxShards();
            if (shard) {
                FallingBlockEntity block = FallingBlockEntity.fall(server, pos.immutable(), state);
                if (block != null) {
                    block.setNoGravity(true);
                    block.noPhysics = true;
                    block.disableDrop();
                    int slot = this.slot++;
                    this.eaten.put(block.getUUID(), new Shard(slot * 0.34D, slot));
                }
            } else {
                server.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
            taken++;
        }

        if (taken > 0 && this.life % 6 == 0) {
            server.playSound(null, getX(), getY(), getZ(), TNSounds.BLOCK_BREAK.get(),
                SoundSource.BLOCKS, 0.55F, 0.85F + this.random.nextFloat() * 0.35F);
        }
    }

    private void dragEaten() {
        if (this.eaten.isEmpty()) return;
        ServerLevel server = (ServerLevel) this.level();
        this.ribbon += 0.055D;

        for (Map.Entry<UUID, Shard> entry : this.eaten.entrySet()) {
            Entity found = server.getEntity(entry.getKey());
            if (!(found instanceof FallingBlockEntity block) || block.isRemoved()) continue;

            Shard shard = entry.getValue();
            int slot = shard.slot();
            double angle = shard.angle() + this.ribbon;
            double radius = 1.22D + 0.085D * (slot % 7);
            double height = 0.30D * Math.sin(slot * 0.7D);

            block.time = 0;
            block.setDeltaMovement(Vec3.ZERO);
            block.setPos(
                getX() + Math.cos(angle) * radius,
                getY() + height + 0.15D,
                getZ() + Math.sin(angle) * radius);
            block.hurtMarked = true;
        }
    }

    private void release(ServerLevel server) {
        if (this.eaten.isEmpty()) return;

        int index = 0;
        for (UUID id : this.eaten.keySet()) {
            Entity found = server.getEntity(id);
            if (!(found instanceof FallingBlockEntity block) || block.isRemoved()) continue;

            double angle = index * 2.399963D;
            double radius = 0.14D + 0.34D * Math.sqrt((index % 14) / 14.0D);
            block.setNoGravity(false);
            block.noPhysics = false;
            block.time = 0;
            block.setPos(
                getX() + Math.cos(angle) * radius,
                getY() + 0.15D + (index % 4) * 0.42D,
                getZ() + Math.sin(angle) * radius);
            block.setDeltaMovement(0.0D, -0.02D, 0.0D);
            block.hurtMarked = true;
            index++;
        }
        this.eaten.clear();
    }

    private void damage(LivingEntity owner) {
        float amount = TrueNinsConfig.aoDamageAmount();
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class,
            this.getBoundingBox().inflate(2.4D), e -> e != owner && e.isAlive() && !isProtected(e));
        for (LivingEntity target : targets) {
            OrbDamage.apply(target, this, owner, amount, SPELL_ID);
        }
    }

    private void intake(ServerLevel server) {
        if (this.life % 2 != 0) return;

        int arms = 2;
        int cycle = 12;
        double phase = (this.life % cycle) / (double) cycle;
        double maxRadius = 2.6D;

        for (int arm = 0; arm < arms; arm++) {
            double p = (phase + arm / (double) arms) % 1.0D;
            double radius = maxRadius * (1.0D - p);
            double angle = this.life * 0.34D + arm * Math.PI;
            server.sendParticles(TNParticles.MOTE_TIDE.get(),
                getX() + Math.cos(angle) * radius,
                getY() + 0.45D * (radius / maxRadius) - 0.18D,
                getZ() + Math.sin(angle) * radius,
                1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private static boolean isProtected(LivingEntity entity) {
        return entity instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0D * 128.0D;
    }
}
