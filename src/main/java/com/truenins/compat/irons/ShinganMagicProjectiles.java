package com.truenins.compat.irons;

import com.truenins.TrueNinsConfig;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Loaded only when Iron's Spells is present. */
public final class ShinganMagicProjectiles {

    private static final List<EntityType<? extends AbstractMagicProjectile>> TYPES = List.of(
        EntityRegistry.FIREBOLT_PROJECTILE.get(),
        EntityRegistry.MAGIC_ARROW_PROJECTILE.get(),
        EntityRegistry.MAGIC_MISSILE_PROJECTILE.get(),
        EntityRegistry.ICICLE_PROJECTILE.get(),
        EntityRegistry.BLOOD_SLASH_PROJECTILE.get(),
        EntityRegistry.LIGHTNING_LANCE_PROJECTILE.get(),
        EntityRegistry.WITHER_SKULL_PROJECTILE.get(),
        EntityRegistry.SMALL_FIREBALL_PROJECTILE.get(),
        EntityRegistry.MAGIC_FIREBALL.get()
    );

    private ShinganMagicProjectiles() {}

    public static void summon(ServerLevel level, Vec3 center, LivingEntity target, LivingEntity caster) {
        AbstractMagicProjectile projectile = TYPES.get(level.random.nextInt(TYPES.size())).create(level);
        if (projectile == null) return;

        Vec3 from = center.add(
            (level.random.nextDouble() - 0.5D) * 6.0D,
            6.0D + level.random.nextDouble() * 6.0D,
            (level.random.nextDouble() - 0.5D) * 6.0D);
        projectile.setPos(from.x, from.y, from.z);
        projectile.setOwner(caster);
        projectile.shoot(target.getEyePosition().subtract(from).normalize());
        projectile.setDeltaMovement(projectile.getDeltaMovement().scale(TrueNinsConfig.shinganAttackProjectileSpeed()));
        projectile.setDamage(6.0F + level.random.nextFloat() * 6.0F);
        level.addFreshEntity(projectile);
    }
}
