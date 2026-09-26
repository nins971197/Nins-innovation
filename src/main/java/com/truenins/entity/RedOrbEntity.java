package com.truenins.entity;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RedOrbEntity extends TNOrbEntity {

    public static final String SPELL_ID = "truenins:he";
    private static final int EXPLODE_TICKS = 18;

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
        this.launchFrom = handPos(owner);
        this.setPos(this.launchFrom.x, this.launchFrom.y, this.launchFrom.z);
        setOrbScale(0.16F);
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
                moveToPos(this.launchFrom);
            }
            server.sendParticles(TNParticles.MOTE_EMBER.get(), getX(), getY(), getZ(),
                1, 0.22D, 0.22D, 0.22D, 0.02D);
            return;
        }

        setOrbScale(1.0F);

        if (!this.flying) {
            this.flying = true;
            server.playSound(null, getX(), getY(), getZ(),
                TNSounds.HE_FLY.get(), SoundSource.BLOCKS, 1.1F, 1.0F);
        }

        double speed = TrueNinsConfig.heSpeed();
        moveToPos(this.position().add(this.launchDir.scale(speed)));
        this.travelled += speed;

        carve(server);
        strike(owner);
        trail(server);

        if (this.travelled > TrueNinsConfig.heMaxDistance() || this.life > TrueNinsConfig.heMaxLifeTicks()) {
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

        if (this.exploding <= 2) {
            shell(server, TNParticles.MOTE_STAR.get(), 0.4D + blast * 0.10D, 6, 0.05D);
        }
        shell(server, TNParticles.MOTE_EMBER.get(), radius, 12, 0.06D);
        if (this.exploding > 4) {
            shell(server, TNParticles.MOTE_EMBER.get(),
                radius * 1.28D, 7, 0.10D);
        }

        if (this.exploding <= 4) {
            double ring = 0.6D + blast * wave * 0.85D;
            int points = 12;
            for (int i = 0; i < points; i++) {
                double angle = i * (Math.PI * 2.0D / points);
                server.sendParticles(TNParticles.MOTE_EMBER.get(),
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
        BlockPos center = this.blockPosition();
        boolean changed = false;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-radius, -radius, -radius),
            center.offset(radius, radius, radius))) {

            BlockState state = server.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getDestroySpeed(server, pos) < 0.0F) continue;
            server.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            changed = true;
        }

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

        int span = (int) Math.ceil(Math.max(length, radius)) + 1;
        BlockPos center = this.blockPosition();
        Vec3 dir = this.launchDir;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-span, -span, -span),
            center.offset(span, span, span))) {

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

            BlockState state = server.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getDestroySpeed(server, pos) < 0.0F) continue;
            server.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private void strike(LivingEntity owner) {
        float amount = TrueNinsConfig.heDamageAmount();
        double radius = TrueNinsConfig.heHitRadius();
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class,
            this.getBoundingBox().inflate(radius), e -> e != owner && e.isAlive() && !isProtected(e));

        for (LivingEntity target : targets) {
            if (!this.struck.add(target.getUUID())) continue;
            OrbDamage.apply(target, this, owner, amount, SPELL_ID);
        }
    }

    private void trail(ServerLevel server) {
        if (this.life % 2 != 0) return;
        server.sendParticles(TNParticles.MOTE_EMBER.get(), getX(), getY(), getZ(),
            2, 0.34D, 0.34D, 0.34D, 0.05D);
        if (this.life % 12 == 0) {
            server.playSound(null, getX(), getY(), getZ(),
                TNSounds.HE_FLY.get(), SoundSource.BLOCKS, 0.35F, 1.25F);
        }
    }

    private static boolean isProtected(LivingEntity entity) {
        return entity instanceof Player player && (player.isCreative() || player.isSpectator());
    }
}
