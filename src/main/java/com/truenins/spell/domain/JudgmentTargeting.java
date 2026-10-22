package com.truenins.spell.domain;

import com.truenins.entity.domain.JudgmentDomainEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/** A single unobstructed eye ray. Never silently selects a different target on cast completion. */
public final class JudgmentTargeting {
    private JudgmentTargeting() {}
    public static LivingEntity pick(LivingEntity caster, double range) {
        Vec3 start = caster.getEyePosition(), end = start.add(caster.getLookAngle().scale(range));
        end = caster.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, caster)).getLocation();
        LivingEntity best = null;
        double nearest = start.distanceToSqr(end);
        for (LivingEntity candidate : caster.level().getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().expandTowards(end.subtract(start)).inflate(1), e -> valid(caster, e, range))) {
            var hit = candidate.getBoundingBox().inflate(.18).clip(start, end);
            if (hit.isPresent() && start.distanceToSqr(hit.get()) <= nearest) {
                best = candidate;
                nearest = start.distanceToSqr(hit.get());
            }
        }
        return best;
    }
    public static boolean valid(LivingEntity caster, LivingEntity target, double range) {
        return target != null && target != caster && target.isAlive() && target.level() == caster.level()
            && !(target instanceof Player p && p.isSpectator()) && target.isPickable()
            && target.distanceToSqr(caster) <= range * range && caster.hasLineOfSight(target);
    }
    public static boolean available(LivingEntity caster, LivingEntity target) {
        return !JudgmentControl.isAffected(caster) && !JudgmentControl.isAffected(target)
            && caster.level().getEntitiesOfClass(JudgmentDomainEntity.class,
                caster.getBoundingBox().inflate(JudgmentDomainEntity.FIELD_RADIUS * 2),
                d -> d.isAlive() && !d.isClosing()).isEmpty();
    }
}
