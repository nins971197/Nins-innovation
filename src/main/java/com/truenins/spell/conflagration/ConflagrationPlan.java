package com.truenins.spell.conflagration;

import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.UUID;

/** Immutable server-authored choreography. All times are world ticks, independent of FPS. */
public record ConflagrationPlan(UUID id, long start, Vec3 center, float blastRadius, List<Strike> strikes) {
    public static final int DURATION = 300;
    public static final int BREAKTHROUGH_START = 180;
    public static final int BREAKTHROUGH_INTERVAL = 9;
    public static final int BREAKTHROUGH_PULSES = 3;
    public ConflagrationPlan { strikes = List.copyOf(strikes); }
    public record Strike(Vec3 from, Vec3 to, int launch, int flight, float size, int seed, boolean giant) {
        public int impact() { return launch + flight; }
        public Vec3 head(float age) {
            double t = Math.max(0, Math.min(1, (age - launch) / flight));
            // Rain accelerates. The giant descends steadily so the cloud breach remains visible
            // for several seconds instead of revealing its entire underside in the final frames.
            return from.lerp(to, giant ? t : t * t);
        }
    }
}
