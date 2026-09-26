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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlueOrbEntity extends TNOrbEntity {

    public static final String SPELL_ID = "truenins:ao";
    public static final Map<UUID, BlueOrbEntity> CHARGING = new HashMap<>();

    private static final int LAUNCH_TICKS = 8;
    private static final int CHARGE_TIMEOUT = 60;

    private final Map<UUID, Shard> eaten = new LinkedHashMap<>();
    private int slot;
    private double ribbon;
    private int chargeTicks = 20;
    private boolean launched;
    private int launchLife;
    private double travelled;
    private Vec3 launchDir = new Vec3(0.0D, 0.0D, 1.0D);
    private Vec3 launchFrom = Vec3.ZERO;
    private Vec3 center = Vec3.ZERO;

    private record Shard(double angle, int slot, double distance) {}

    public BlueOrbEntity(EntityType<? extends BlueOrbEntity> type, Level level) {
        super(type, level);
    }

    public BlueOrbEntity(Level level, LivingEntity owner, int chargeTicks) {
        super(TNEntities.BLUE_ORB.get(), level);
        setOwner(owner);
        this.chargeTicks = Math.max(4, chargeTicks);
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

        LivingEntity owner = owner();
        if (owner != null) {
            Vec3 look = owner.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0D, look.z);
            this.launchDir = flat.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : flat.normalize();
            this.center = owner.position();
        }
        setOrbScale(1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        this.life++;
        ServerLevel server = (ServerLevel) this.level();
        LivingEntity owner = owner();

        if (!this.launched) {
            if (this.life > this.chargeTicks + CHARGE_TIMEOUT) {
                if (this.ownerId != null) CHARGING.remove(this.ownerId);
                this.discard();
                return;
            }
            if (owner != null) this.center = owner.position();
            double lap = (this.life / (double) this.chargeTicks) * (Math.PI * 2.0D);
            double radius = TrueNinsConfig.aoChargeRadius();
            moveToPos(new Vec3(
                this.center.x + Math.cos(lap) * radius,
                this.center.y + 1.05D + Math.sin(lap * 2.0D) * 0.20D,
                this.center.z + Math.sin(lap) * radius));
            setOrbScale(Math.min(1.0F, 0.20F + 0.80F * (this.life / (float) this.chargeTicks)));
        } else {
            int t = this.life - this.launchLife;
            if (t >= TrueNinsConfig.aoDurationTicks()) {
                dissipate(server);
                return;
            }
            if (t <= LAUNCH_TICKS) {
                double e = 1.0D - Math.pow(1.0D - t / (double) LAUNCH_TICKS, 2.4D);
                Vec3 next = this.launchFrom.add(this.launchDir.scale(3.8D * e));
                this.travelled = next.distanceTo(this.launchFrom);
                moveToPos(next);
            } else if (this.travelled < TrueNinsConfig.aoMaxDistance()) {
                double speed = TrueNinsConfig.aoFlightSpeed();
                this.travelled += speed;
                moveToPos(this.position().add(this.launchDir.scale(speed)));
            } else {
                moveToPos(new Vec3(getX(), getY() + Math.sin(this.life * 0.12D) * 0.02D, getZ()));
            }
        }

        pull(owner);
        dragEaten(server);
        consume(server);
        if (this.life % TrueNinsConfig.aoDamageIntervalTicks() == 0) damage(owner);
        intake(server);

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
        int floor = this.launched ? Integer.MIN_VALUE : (int) Math.floor(this.center.y) + 1;
        int taken = 0;

        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-radius, -radius, -radius),
            center.offset(radius, radius, radius))) {

            if (taken >= budget) break;
            if (!this.launched && pos.getY() < floor) continue;
            BlockState state = server.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getDestroySpeed(server, pos) < 0.0F) continue;
            if (state.hasBlockEntity()) continue;

            if (this.eaten.size() < TrueNinsConfig.aoMaxShards()) {
                OrbShardEntity shard = new OrbShardEntity(server,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, state);
                server.addFreshEntity(shard);
                int index = this.slot++;
                double dx = shard.getX() - getX();
                double dz = shard.getZ() - getZ();
                this.eaten.put(shard.getUUID(),
                    new Shard(Math.atan2(dz, dx), index, Math.max(0.5D, Math.sqrt(dx * dx + dz * dz))));
            }
            server.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            taken++;
        }

        if (taken > 0 && this.life % 6 == 0) {
            server.playSound(null, getX(), getY(), getZ(), TNSounds.BLOCK_BREAK.get(),
                SoundSource.BLOCKS, 0.55F, 0.85F + this.random.nextFloat() * 0.35F);
        }
    }

    private void dragEaten(ServerLevel server) {
        if (this.eaten.isEmpty()) return;
        this.ribbon += 0.13D;

        java.util.Iterator<Map.Entry<UUID, Shard>> it = this.eaten.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Shard> entry = it.next();
            Entity found = server.getEntity(entry.getKey());
            if (!(found instanceof OrbShardEntity shard)) {
                it.remove();
                continue;
            }

            Shard state = entry.getValue();
            int index = state.slot();
            double hold = 1.15D + 0.075D * (index % 7);
            double distance = state.distance() + (hold - state.distance()) * 0.45D;
            double angle = state.angle() + this.ribbon;
            double height = 0.28D * Math.sin(index * 0.7D);

            shard.setPos(
                getX() + Math.cos(angle) * distance,
                getY() + height,
                getZ() + Math.sin(angle) * distance);
            entry.setValue(new Shard(angle, index, distance));
        }
    }

    private void dissipate(ServerLevel server) {
        BlockPos origin = BlockPos.containing(getX(), groundLevel(server), getZ());
        int index = 0;

        for (UUID id : this.eaten.keySet()) {
            Entity found = server.getEntity(id);
            if (!(found instanceof OrbShardEntity shard)) continue;
            BlockState state = shard.blockState();
            shard.discard();
            if (state.isAir()) continue;

            double angle = index * 2.399963D;
            double spread = 0.26D * Math.sqrt(index);
            int layer = (int) Math.floor(spread / 0.85D);
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * spread);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * spread);
            place(server, new BlockPos(x, origin.getY() + layer, z), state);
            index++;
        }
        this.eaten.clear();

        server.sendParticles(TNParticles.MOTE_TIDE.get(), getX(), getY(), getZ(),
            14, 0.45D, 0.45D, 0.45D, 0.18D);
        server.playSound(null, getX(), getY(), getZ(),
            TNSounds.HE_EXPLODE.get(), SoundSource.BLOCKS, 0.45F, 1.7F);
        this.discard();
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
        for (int i = 0; i < 8; i++) {
            BlockState at = server.getBlockState(cursor);
            if (at.isAir() || at.canBeReplaced()) {
                server.setBlock(cursor, state, 3);
                return;
            }
            cursor.move(0, 1, 0);
        }
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
