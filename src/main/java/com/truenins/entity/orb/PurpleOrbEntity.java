package com.truenins.entity.orb;

import com.truenins.TrueNinsConfig;
import com.truenins.register.TNEntities;
import com.truenins.register.TNSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Hollow Purple: a stationary convergence of Ao and He followed by one blast. */
public final class PurpleOrbEntity extends TNOrbEntity {
    /** The remaining time is visual gas dissipation; damage still occurs exactly once. */
    private static final int RELEASE_TICKS = 90;
    public static final String SPELL_ID = "truenins:purple";
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_PHASE =
        net.minecraft.network.syncher.SynchedEntityData.defineId(PurpleOrbEntity.class,
            net.minecraft.network.syncher.EntityDataSerializers.INT);
    private final Set<UUID> struck = new HashSet<>();
    private int phase;
    private boolean detonated;

    public PurpleOrbEntity(EntityType<? extends PurpleOrbEntity> type, Level level) { super(type, level); }

    public PurpleOrbEntity(ServerLevel level, LivingEntity owner) {
        this(TNEntities.PURPLE_ORB.get(), level);
        setOwner(owner);
        Vec3 point = owner.getEyePosition().add(owner.getLookAngle().normalize().scale(TrueNinsConfig.purpleSpawnDistance()));
        setPos(point.x, point.y, point.z);
        setYRot(owner.getYRot()); setXRot(owner.getXRot()); setOrbScale(.18F);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_PHASE, 0);
    }

    public int phase() { return entityData.get(DATA_PHASE); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        life++;
        LivingEntity owner = owner();
        if (owner == null || !owner.isAlive()) { discard(); return; }
        int fusion = TrueNinsConfig.purpleFusionTicks();
        if (!detonated) {
            if (life <= fusion) {
                phase = life;
                entityData.set(DATA_PHASE, phase);
                setOrbScale(.18F + .82F * Math.min(1F, life / (float)fusion));
            } else {
                detonated = true;
                entityData.set(DATA_PHASE, -1);
                setExplodeProgress(0);
                explode((ServerLevel) level(), owner);
            }
        } else {
            int progress = explodeProgress() + 1;
            setExplodeProgress(progress);
            if (progress >= RELEASE_TICKS) discard();
        }
    }

    private void explode(ServerLevel server, LivingEntity owner) {
        server.playSound(null, getX(), getY(), getZ(), TNSounds.HE_EXPLODE.get(), SoundSource.BLOCKS, 3.5F, .72F);
        boolean terrain = TrueNinsConfig.purpleBreakBlocks()
            && server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        // The Level overload keeps Forge explosion hooks and normal block updates without
        // sending ServerLevel's vanilla smoke packet. Synced phase drives the reference VFX;
        // strikeExplosionTargets marks each custom knockback for normal entity synchronization.
        server.explode(this, null, null, getX(), getY(), getZ(), (float)TrueNinsConfig.purpleBlastRadius(),
            false, terrain ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE, false);
    }

    /** Called with the targets left by Forge explosion hooks, before vanilla applies damage. */
    public void strikeExplosionTargets(java.util.List<net.minecraft.world.entity.Entity> entities) {
        LivingEntity caster = owner();
        if (caster == null) return;
        double radius = TrueNinsConfig.purpleBlastRadius();
        for (var entity : entities) {
            if (!(entity instanceof LivingEntity target) || target == caster || !target.isAlive() || isProtected(target)) continue;
            if (target.distanceToSqr(position()) > radius * radius || !struck.add(target.getUUID())) continue;
            com.truenins.spell.OrbDamage.apply(target, this, caster, TrueNinsConfig.purpleDamageAmount(), SPELL_ID);
            Vec3 away = target.position().subtract(position()).normalize().scale(.9D);
            target.setDeltaMovement(target.getDeltaMovement().add(away));
            target.hurtMarked = true;
        }
    }

}
