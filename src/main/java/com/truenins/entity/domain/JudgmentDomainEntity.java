package com.truenins.entity.domain;

import com.truenins.gameplay.state.JudgmentPenalty;
import com.truenins.register.TNEntities;
import com.truenins.register.TNSounds;
import com.truenins.spell.domain.JudgmentRules;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;
import java.util.function.Consumer;

/** One caster, one immutable defendant, one verdict. All architecture is a reversible visual projection. */
public class JudgmentDomainEntity extends Entity implements DomainRegistry.Domain {
    public static final int BLACKOUT_END = 24, BUILD_END = 132, VERDICT_TICK = 170, STRIKE_TICK = 212, CLOSE_TICKS = 40;
    public static final float INNER_RADIUS = 11f, COURT_RADIUS = 21f, FIELD_RADIUS = 24f;
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(JudgmentDomainEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> CLOSING = SynchedEntityData.defineId(JudgmentDomainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VERDICT = SynchedEntityData.defineId(JudgmentDomainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(JudgmentDomainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CASTER = SynchedEntityData.defineId(JudgmentDomainEntity.class, EntityDataSerializers.INT);
    private static final UUID HEALTH_ID = UUID.fromString("2899ade0-6b41-4a3a-bc21-c55657b02052");
    private UUID casterId, targetId;
    private Vec3 casterOrigin, targetOrigin;
    private float casterYaw, casterPitch, targetYaw, targetPitch;
    private boolean positioned, resolved, returned;
    private int age;
    private long domainSeq;
    private boolean registered;

    public JudgmentDomainEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true; noCulling = true; setNoGravity(true);
    }
    public JudgmentDomainEntity(ServerLevel level, LivingEntity caster, LivingEntity target) {
        this(TNEntities.JUDGMENT_DOMAIN.get(), level);
        casterId = caster.getUUID(); targetId = target.getUUID();
        casterOrigin = caster.position(); targetOrigin = target.position();
        casterYaw = caster.getYRot(); casterPitch = caster.getXRot();
        targetYaw = target.getYRot(); targetPitch = target.getXRot();
        Vec3 toward = target.position().subtract(caster.position()).multiply(1, 0, 1).normalize();
        if (toward.lengthSqr() < .01) toward = Vec3.directionFromRotation(0, casterYaw);
        Vec3 center = caster.position().add(toward.scale(Math.min(4, caster.distanceTo(target) * .5)));
        setPos(center); setYRot((float)Math.toDegrees(Math.atan2(toward.x, toward.z)));
        // Find clear standing volume without replacing blocks, even in rooms or caves.
        for (int y = 0; y <= 8; y++) {
            setPos(center.add(0, y, 0));
            if (clearAt(caster, stand(false)) && clearAt(target, stand(true))) break;
            if (y == 8) setPos(center);
        }
        entityData.set(START, level.getGameTime());
        entityData.set(TARGET, target.getId()); entityData.set(CASTER, caster.getId());
    }
    @Override protected void defineSynchedData() {
        entityData.define(START, 0L); entityData.define(CLOSING, -1);
        entityData.define(VERDICT, JudgmentRules.UNDECIDED); entityData.define(TARGET, -1); entityData.define(CASTER, -1);
    }
    private boolean clearAt(LivingEntity e, Vec3 at) {
        return level().noCollision(e, e.getBoundingBox().move(at.subtract(e.position())));
    }
    public float visualAge(float partial) { return Math.max(0, level().getGameTime() - entityData.get(START) + partial); }
    public float closingProgress(float partial) {
        return isClosing() ? Mth.clamp((visualAge(partial) - entityData.get(CLOSING)) / CLOSE_TICKS, 0, 1) : 0;
    }
    public float visibility(float partial) { return 1 - smooth(closingProgress(partial)); }
    public static float smooth(float t) { t = Mth.clamp(t, 0, 1); return t * t * (3 - 2 * t); }
    public boolean isClosing() { return entityData.get(CLOSING) >= 0; }
    public int verdict() { return entityData.get(VERDICT); }
    public Entity defendant() { return level().getEntity(entityData.get(TARGET)); }
    public Entity advocate() { return level().getEntity(entityData.get(CASTER)); }
    public boolean participates(UUID id) { return id.equals(casterId) || id.equals(targetId); }
    public Vec3 local(double x, double y, double z) {
        return new Vec3(x, y, z).yRot((float)Math.toRadians(getYRot()));
    }
    public Vec3 stand(boolean defendant) { return position().add(local(0, .04, defendant ? 4 : -4)); }
    public boolean affects(LivingEntity e) {
        return isAlive() && !isClosing() && e.isAlive() && !(e instanceof Player p && p.isSpectator())
            && (e.getId() == entityData.get(TARGET) || e.getId() == entityData.get(CASTER)
                || e.distanceToSqr(position().add(0, 3, 0)) < FIELD_RADIUS * FIELD_RADIUS);
    }
    private LivingEntity living(UUID id) {
        if (!(level() instanceof ServerLevel server) || id == null) return null;
        Entity e = server.getEntity(id);
        return e instanceof LivingEntity l && l.isAlive() ? l : null;
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        // Registration also closes any other domain this caster still had open.
        if (!registered) { registered = true; DomainRegistry.register(this); }
        age++;
        LivingEntity caster = living(casterId), target = living(targetId);
        if (!isClosing() && (caster == null || target == null)) beginClose();
        // Gavel cues are scheduled locally against START, together with the contact animation.
        if (!isClosing() && age == BLACKOUT_END) {
            if (caster != null && target != null && clearAt(caster, stand(false)) && clearAt(target, stand(true))) {
                relocate(caster, stand(false), -getYRot(), 0);
                relocate(target, stand(true), 180 - getYRot(), 0);
                positioned = true;
            } else beginClose();
        }
        if (!isClosing() && age > BLACKOUT_END && age < BUILD_END && age % 22 == 0)
            sound(SoundEvents.DEEPSLATE_BRICKS_PLACE, .7f, .65f + age / 220f);
        if (!isClosing() && age == VERDICT_TICK && target != null) judge(target);
        if (!isClosing() && verdict() == JudgmentRules.CONFISCATION && age >= VERDICT_TICK + 24) beginClose();
        if (!isClosing() && verdict() == JudgmentRules.DEATH && age == STRIKE_TICK && target != null) execute(target, caster);
        if (!isClosing() && age >= STRIKE_TICK + 26) beginClose();
        if (isClosing() && age >= entityData.get(CLOSING) + CLOSE_TICKS) discard();
    }
    private void judge(LivingEntity target) {
        int verdict = JudgmentRules.kind(target) == JudgmentRules.Kind.INNOCENT ? JudgmentRules.ACQUITTED
            : random.nextBoolean() ? JudgmentRules.CONFISCATION : JudgmentRules.DEATH;
        entityData.set(VERDICT, verdict);
        announce(verdict);
        if (verdict == JudgmentRules.ACQUITTED) { resolved = true; beginClose(); }
        if (verdict == JudgmentRules.CONFISCATION) {
            resolved = true;
            // Confiscation strips 30% of everything the defendant deals for 30 seconds.
            JudgmentPenalty.apply(target);
            if (target instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("ui.truenins.judgment.confiscation.detail",
                    JudgmentPenalty.DURATION_TICKS / 20), true);
            }
        }
    }
    private void execute(LivingEntity target, LivingEntity caster) {
        if (resolved) return;
        resolved = true;
        ServerLevel server = (ServerLevel)level();
        server.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + target.getBbHeight() * .6,
            target.getZ(), 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 26, .45, .8, .45, .22);
        sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, .65f);
        sound(SoundEvents.ANVIL_LAND, .7f, 1.5f);
        switch (JudgmentRules.kind(target)) {
            case PLAYER -> {
                // Ordinary physical damage: armour, resistance, shields and Forge damage hooks still apply.
                var source = new net.minecraft.world.damagesource.DamageSource(
                    server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(caster instanceof Player ? net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK
                            : net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK), this, caster);
                target.hurt(source, target.getMaxHealth() * .60f);
            }
            case BOSS -> {
                var attr = target.getAttribute(Attributes.MAX_HEALTH);
                if (attr != null) {
                    // Each distinct completed death sentence compounds by 30%, persisting in entity NBT.
                    var old = attr.getModifier(HEALTH_ID);
                    double multiplier = (old == null ? 1 : 1 + old.getAmount()) * .70;
                    double minimum = 1 / Math.max(1, attr.getBaseValue());
                    attr.removeModifier(HEALTH_ID);
                    attr.addPermanentModifier(new AttributeModifier(HEALTH_ID, "truenins.judgment_health",
                        Math.max(minimum, multiplier) - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    target.setHealth(Math.min(target.getHealth(), target.getMaxHealth()));
                }
            }
            case HOSTILE -> {
                if (caster != null) target.setLastHurtByMob(caster);
                if (caster instanceof Player p) target.setLastHurtByPlayer(p);
                target.hurt(new net.minecraft.world.damagesource.DamageSource(
                    server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL), this, caster), Float.MAX_VALUE);
            }
            default -> { }
        }
    }
    private void announce(int result) {
        String key = result == JudgmentRules.ACQUITTED ? "acquitted" : result == JudgmentRules.CONFISCATION ? "confiscation" : "death";
        for (ServerPlayer p : ((ServerLevel)level()).players()) if (p.distanceToSqr(position()) < 40 * 40)
            p.displayClientMessage(Component.translatable("ui.truenins.judgment." + key), false);
    }
    public void beginClose() {
        if (!isClosing()) entityData.set(CLOSING, age);
    }
    private void sound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
    private static void relocate(LivingEntity e, Vec3 pos, float yaw, float pitch) {
        if (e.isPassenger()) e.stopRiding();
        e.setDeltaMovement(Vec3.ZERO); e.fallDistance = 0;
        if (e instanceof ServerPlayer p) p.teleportTo(p.serverLevel(), pos.x, pos.y, pos.z, yaw, pitch);
        else { e.teleportTo(pos.x, pos.y, pos.z); e.setYRot(yaw); e.setXRot(pitch); e.setYHeadRot(yaw); }
    }
    private void returnParticipants() {
        if (!positioned || returned || level().isClientSide) return;
        returned = true;
        LivingEntity caster = living(casterId), target = living(targetId);
        // A world edit during the trial must not return anyone inside a newly placed solid block.
        restore(caster, casterOrigin, casterYaw, casterPitch);
        restore(target, targetOrigin, targetYaw, targetPitch);
    }
    private void restore(LivingEntity e, Vec3 origin, float yaw, float pitch) {
        if (e == null || origin == null) return;
        for (int y = 0; y < 8; y++) if (clearAt(e, origin.add(0, y, 0))) {
            relocate(e, origin.add(0, y, 0), yaw, pitch); return;
        }
    }
    // ---------------------------------------------------------------- DomainRegistry.Domain
    @Override public DomainRegistry.Kind domainKind() { return DomainRegistry.Kind.JUDGMENT; }
    @Override public java.util.UUID domainOwnerId() { return casterId; }
    @Override public Entity domainEntity() { return this; }
    @Override public long domainSeq() { return domainSeq; }
    @Override public void setDomainSeq(long seq) { domainSeq = seq; }
    @Override public boolean domainSuppressesControl() { return true; }
    @Override public boolean domainClosing() { return isClosing(); }
    @Override public boolean controlsEntity(LivingEntity e) { return !isClosing() && affects(e); }
    /** One AABB query per tick; participants are added explicitly so being shoved out never frees them. */
    @Override public void collectControlled(ServerLevel level, Consumer<LivingEntity> out) {
        if (isClosing()) return;
        Vec3 center = position().add(0, 3, 0);
        double r2 = FIELD_RADIUS * FIELD_RADIUS;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(FIELD_RADIUS),
                e -> e.isAlive() && !(e instanceof Player p && p.isSpectator()) && e.distanceToSqr(center) < r2)) {
            out.accept(e);
        }
        for (int id : new int[]{entityData.get(TARGET), entityData.get(CASTER)}) {
            Entity e = level.getEntity(id);
            if (e instanceof LivingEntity living && living.isAlive()) out.accept(living);
        }
    }
    /** Full teardown path: the same close animation the verdict uses, and participants are returned
     *  right away so a /stop or a crash mid-trial cannot leave them standing on the court positions. */
    @Override public void closeDomain(String reason) { beginClose(); returnParticipants(); }

    @Override public void remove(RemovalReason reason) { DomainRegistry.unregister(this); returnParticipants(); super.remove(reason); }
    // These short-lived entities are registered noSave. Recover defensively from a manually saved entity.
    @Override protected void readAdditionalSaveData(CompoundTag tag) { beginClose(); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 256 * 256; }
}
