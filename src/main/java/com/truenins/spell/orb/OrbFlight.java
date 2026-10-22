package com.truenins.spell.orb;

import net.minecraft.world.phys.Vec3;

/** Server flight math. Never project a cast onto the horizontal plane. */
public final class OrbFlight {
    private OrbFlight() {}

    public static Vec3 direction(Vec3 look) {
        return look.lengthSqr() < 1.0e-12 ? new Vec3(0, 0, 1) : look.normalize();
    }

    public static double step(double requested, double travelled, double range) {
        return Math.max(0, Math.min(requested, range - travelled));
    }
}
