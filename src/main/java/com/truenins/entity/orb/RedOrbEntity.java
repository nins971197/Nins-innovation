package com.truenins.entity.orb;

import com.truenins.TrueNinsConfig;
import com.truenins.register.TNEntities;
import com.truenins.register.TNParticles;
import com.truenins.register.TNSounds;
import com.truenins.spell.OrbDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RedOrbEntity extends TNOrbEntity {

    public static final String SPELL_ID = "truenins:he";
    private static final net.minecraft.core.particles.DustParticleOptions RED_MOTE =
        new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1f, .025f, .065f), .5f);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_FLYING =
        net.minecraft.network.syncher.SynchedEntityData.defineId(RedOrbEntity.class,
            net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    private Vec3 launchDir = new Vec3(0.0D, 0.0D, 1.0D);
    private Vec3 launchFrom = Vec3.ZERO;
    private double travelled;
    private boolean flying;
    private int exploding = -1;
    private final Set<UUID> struck = new HashSet<>();

    public RedOrbEntity(EntityType<? extends RedOrbEntity> type, Level level) {
        super(type, level);
    }

    public RedOrbEntity(Level level, LivingEntity owner) {
        super(TNEntities.RED_ORB.get(), level);
        setOwner(owner);
        this.launchDir = owner.getLookAngle().normalize();
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
        this.launchFrom = handPos(owner);
        this.setPos(this.launchFrom.x, this.launchFrom.y, this.launchFrom.z);
        setOrbScale(0.16F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLYING, false);
    }

    public boolean visualFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    public Vec3 visualDirection() {
        return Vec3.directionFromRotation(this.getXRot(), this.getYRot());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        this.life++;
        LivingEntity owner = owner();
        ServerLevel server = (ServerLevel) this.level();

        if (this.exploding >= 0) {
            explode(server);
            return;
        }

        if (this.life <= HOLD_TICKS) {
            setOrbScale(growOver(this.life, HOLD_TICKS));
            if (owner != null) {
                this.launchFrom = handPos(owner);
                this.launchDir = owner.getLookAngle().normalize();
                this.setYRot(owner.getYRot());
                this.setXRot(owner.getXRot());
                moveToPos(this.launchFrom);
            }
            server.sendParticles(RED_MOTE, getX(), getY(), getZ(),
                1, 0.22D, 0.22D, 0.22D, 0.02D);
            return;
        }

        setOrbScale(1.0F);

        if (!this.flying) {
            this.flying = true;
            this.entityData.set(DATA_FLYING, true);
            server.playSound(null, getX(), getY(), getZ(),
                TNSounds.HE_FLY.get(), SoundSource.BLOCKS, 1.1F, 1.0F);
        }

        double speed = TrueNinsConfig.heSpeed();
        int steps = Math.max(1, (int) Math.ceil(speed / 0.70D));
        double step = speed / steps;
        for (int i = 0; i < steps; i++) {
            double remaining = TrueNinsConfig.heMaxDistance() - this.travelled;
            if (remaining <= 0.0D) break;
            double distance = Math.min(step, remaining);
            moveToPos(this.position().add(this.launchDir.scale(distance)));
            this.travelled += distance;
            carve(server);
            strike(owner);
        }
        trail(server);

        if (this.travelled >= TrueNinsConfig.heMaxDistance() || this.life > TrueNinsConfig.heMaxLifeTicks()) {
            this.exploding = 0;
            setExplodeProgress(0);
            server.playSound(null, getX(), getY(), getZ(), TNSounds.HE_EXPLODE.get(),
                SoundSource.BLOCKS, 4.0F, 1.0F);
            if (owner instanceof net.minecraft.server.level.ServerPlayer player
                && player.distanceToSqr(this) > 400.0D) {
                server.playSound(null, player.getX(), player.getY(), player.getZ(),
                    TNSounds.HE_EXPLODE.get(), SoundSource.BLOCKS, 1.1F, 1.0F);
            }
        }
    }

    private void explode(ServerLevel server) {
        this.exploding++;
        setExplodeProgress(this.exploding);
        double progress = Math.min(1.0D, this.exploding / (double) EXPLODE_TICKS);
        double wave = Math.pow(progress, 0.42D);

        carveTear(server, wave);

        double blast = TrueNinsConfig.heBlastRadius();
        double radius = 0.4D + blast * wave;
        repel(owner(), radius);

        if (this.exploding <= 2) {
            shell(server, TNParticles.MOTE_STAR.get(), 0.4D + blast * 0.10D, 6, 0.05D);
        }
        shell(server, RED_MOTE, radius, 12, 0.06D);
        if (this.exploding > 4) {
            shell(server, RED_MOTE,
                radius * 1.28D, 7, 0.10D);
        }

        if (this.exploding <= 4) {
            double ring = 0.6D + blast * wave * 0.85D;
            int points = 12;
            for (int i = 0; i < points; i++) {
                double angle = i * (Math.PI * 2.0D / points);
                server.sendParticles(RED_MOTE,
                    getX() + Math.cos(angle) * ring,
                    getY() + 0.18D,
                    getZ() + Math.sin(angle) * ring,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }

        if (this.exploding == 6 || this.exploding == 12) {
            server.playSound(null, getX(), getY(), getZ(), TNSounds.BLOCK_BREAK.get(),
                SoundSource.BLOCKS, 0.9F, 0.55F + this.random.nextFloat() * 0.3F);
        }

        if (this.exploding >= EXPLODE_TICKS) this.discard();
    }

    private void shell(ServerLevel server, net.minecraft.core.particles.ParticleOptions type,
                       double radius, int count, double jitter) {
        double golden = Math.PI * (3.0D - Math.sqrt(5.0D));
        for (int i = 0; i < count; i++) {
            double y = 1.0D - 2.0D * (i + 0.5D) / count;
            double ring = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
            double phi = i * golden + this.life * 0.21D;
            server.sendParticles(type,
                getX() + Math.cos(phi) * ring * radius,
                getY() + y * radius,
                getZ() + Math.sin(phi) * ring * radius,
                1, jitter, jitter, jitter, 0.0D);
        }
    }

    private void carve(ServerLevel server) {
        if (!TrueNinsConfig.heBreakBlocks()) return;

        int radius = TrueNinsConfig.heBreakRadius();
        double limit = (radius + 0.5D) * (radius + 0.5D);
        BlockPos center = this.blockPosition();
        boolean changed = false;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-radius, -radius, -radius),
            center.offset(radius, radius, radius))) {

            double dx = pos.getX() + 0.5D - getX();
            double dy = pos.getY() + 0.5D - getY();
            double dz = pos.getZ() + 0.5D - getZ();
            if (dx * dx + dy * dy + dz * dz > limit) continue;
            if (!server.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;

            BlockState state = server.getBlockState(pos);
            if (state.isAir()) continue;
            if (!mayBreak(server, pos, state)) continue;
            if (!TrueNinsConfig.heBreakContainers() && state.hasBlockEntity()) continue;
            server.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            changed = true;        }

        if (changed && this.life % 4 == 0) {
            server.playSound(null, getX(), getY(), getZ(), TNSounds.BLOCK_BREAK.get(),
                SoundSource.BLOCKS, 0.85F, 0.7F + this.random.nextFloat() * 0.4F);
        }
    }

    private void carveTear(ServerLevel server, double progress) {
        if (!TrueNinsConfig.heBreakBlocks()) return;

        double length = TrueNinsConfig.heTearLength() * progress;
        double radius = TrueNinsConfig.heTearRadius() * progress;
        if (length < 0.5D || radius < 0.5D) return;

        BlockPos center = this.blockPosition();
        Vec3 dir = this.launchDir;
        // Project the oriented ellipsoid onto each world axis instead of scanning a cube.
        int spanX = (int) Math.ceil(Math.sqrt(length * length * dir.x * dir.x
            + radius * radius * (1 - dir.x * dir.x)) * 1.3D) + 1;
        int spanY = (int) Math.ceil(Math.sqrt(length * length * dir.y * dir.y
            + radius * radius * (1 - dir.y * dir.y)) * 1.3D) + 1;
        int spanZ = (int) Math.ceil(Math.sqrt(length * length * dir.z * dir.z
            + radius * radius * (1 - dir.z * dir.z)) * 1.3D) + 1;
        int torn = 0;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-spanX, -spanY, -spanZ),
            center.offset(spanX, spanY, spanZ))) {

            double px = pos.getX() + 0.5D - getX();
            double py = pos.getY() + 0.5D - getY();
            double pz = pos.getZ() + 0.5D - getZ();
            double axial = px * dir.x + py * dir.y + pz * dir.z;
            double rx = px - dir.x * axial;
            double ry = py - dir.y * axial;
            double rz = pz - dir.z * axial;
            double radial = Math.sqrt(rx * rx + ry * ry + rz * rz);

            double a = axial / length;
            double r = radial / radius;
            double jag = 0.34D * Math.sin(axial * 1.7D + radial * 3.3D + (pos.getX() + pos.getZ()) * 0.7D)
                       + 0.24D * Math.sin(axial * 0.9D - radial * 5.1D + pos.getY() * 1.3D);

            if (a * a + r * r > 1.0D + jag) continue;
            if (!server.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;

            BlockState state = server.getBlockState(pos);
            if (state.isAir()) continue;
            if (!mayBreak(server, pos, state)) continue;
            if (!TrueNinsConfig.heBreakContainers() && state.hasBlockEntity()) continue;
            server.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            torn++;
        }

        if (torn > 0) {
            server.playSound(null, getX(), getY(), getZ(), TNSounds.BLOCK_BREAK.get(),
                SoundSource.BLOCKS, 1.0F, 0.42F + this.random.nextFloat() * 0.5F);
            if (torn > 24) {
                server.playSound(null, getX(), getY(), getZ(), TNSounds.BLOCK_BREAK.get(),
                    SoundSource.BLOCKS, 0.8F, 0.28F + this.random.nextFloat() * 0.3F);
            }
        }
    }

    private void strike(LivingEntity owner) {
        repel(owner, TrueNinsConfig.heHitRadius());
    }

    private void repel(LivingEntity owner, double radius) {
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(radius),
                e -> e != owner && e.isAlive() && !isProtected(e))) {
            Vec3 outward = target.position().add(0, target.getBbHeight() * 0.5D, 0)
                .subtract(this.position());
            double reach = radius + target.getBbWidth() * 0.5D;
            if (outward.lengthSqr() > reach * reach || !this.struck.add(target.getUUID())) continue;
            OrbDamage.apply(target, this, owner, TrueNinsConfig.heDamageAmount(), SPELL_ID);
            Vec3 direction = outward.lengthSqr() < 1.0E-6D ? this.launchDir : outward.normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(direction.scale(1.35D)).add(0, 0.18D, 0));
            target.hurtMarked = true;
        }
    }

    private void trail(ServerLevel server) {
        if (this.life % 2 != 0) return;

        int count = 2 + this.random.nextInt(4);
        for (int i = 0; i < count; i++) {
            double jx = (this.random.nextDouble() - 0.5D) * 1.15D;
            double jy = (this.random.nextDouble() - 0.5D) * 1.15D;
            double jz = (this.random.nextDouble() - 0.5D) * 1.15D;
            server.sendParticles(
                RED_MOTE,
                getX() + jx, getY() + jy, getZ() + jz,
                1, 0.0D, 0.0D, 0.0D, 0.03D);
        }

        if (this.random.nextInt(3) == 0) {
            server.sendParticles(TNParticles.MOTE_STAR.get(),
                getX(), getY(), getZ(), 1, 0.5D, 0.5D, 0.5D, 0.0D);
        }

        if (this.life % 12 == 0) {
            server.playSound(null, getX(), getY(), getZ(),
                TNSounds.HE_FLY.get(), SoundSource.BLOCKS, 0.35F, 1.25F);
        }
    }
}
