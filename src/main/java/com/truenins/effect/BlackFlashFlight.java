package com.truenins.effect;

import com.truenins.TrueNinsConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BlackFlashFlight {

    private record Flight(Vec3 dir, int ticks) {}

    private static final Map<UUID, Flight> ACTIVE = new HashMap<>();

    private BlackFlashFlight() {}

    public static void launch(LivingEntity target, Vec3 from) {
        Vec3 dir = target.position().subtract(from);
        dir = new Vec3(dir.x, 0.0D, dir.z);
        if (dir.lengthSqr() < 1.0E-4D) dir = new Vec3(0.0D, 0.0D, 1.0D);
        ACTIVE.put(target.getUUID(), new Flight(dir.normalize(), TrueNinsConfig.blackFlashLaunchTicks()));
    }

    public static void tick(LivingEntity entity) {
        Flight flight = ACTIVE.get(entity.getUUID());
        if (flight == null) return;
        if (flight.ticks() <= 0 || entity.isDeadOrDying()) {
            ACTIVE.remove(entity.getUUID());
            return;
        }
        double speed = TrueNinsConfig.blackFlashLaunchSpeed();
        entity.setDeltaMovement(flight.dir().x * speed, TrueNinsConfig.blackFlashLaunchLift(),
            flight.dir().z * speed);
        entity.fallDistance = 0.0F;
        entity.hurtMarked = true;
        ACTIVE.put(entity.getUUID(), new Flight(flight.dir(), flight.ticks() - 1));
    }

    public static void clear(LivingEntity entity) {
        ACTIVE.remove(entity.getUUID());
    }
}
