package com.truenins.validation;

import com.truenins.spell.orb.OrbFlight;
import net.minecraft.world.phys.Vec3;

/** Regression checks for the actual trajectory math, without third-party test dependencies. */
public final class OrbFlightChecks {
    public static void main(String[] args) {
        Vec3[] directions = {new Vec3(1, 0, 0), new Vec3(1, 1, 1), new Vec3(-1, 1, -1),
            new Vec3(1, -1, -1), new Vec3(0, 1, 0), new Vec3(0, -1, 0), new Vec3(0, .98, .02)};
        for (Vec3 look : directions) {
            Vec3 dir = OrbFlight.direction(look);
            require(Math.abs(dir.length() - 1) < 1e-10, "Non-unit launch direction: " + look);
            require(dir.dot(look.normalize()) > .999999999, "Pitch lost: " + look);
            Vec3 start = new Vec3(4.5, 64, -9.5);
            Vec3 end = start;
            double travelled = 0;
            for (int tick = 0; tick < 80; tick++) {
                double step = OrbFlight.step(.73, travelled, 18);
                travelled += step;
                end = end.add(dir.scale(step));
            }
            require(Math.abs(end.distanceTo(start) - 18) < 1e-9, "Wrong maximum range");
            require(end.subtract(start).normalize().dot(look.normalize()) > .999999999, "Trajectory drift");
        }
        require(OrbFlight.direction(Vec3.ZERO).equals(new Vec3(0, 0, 1)), "Zero-vector fallback");
        require(OrbFlight.step(3, 17, 18) == 1, "Overshot maximum range");
        require(OrbFlight.step(3, 19, 18) == 0, "Negative motion past maximum range");
        System.out.println("OrbFlight: 7 directions, full trajectories, zero vector and range caps passed.");
    }
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
