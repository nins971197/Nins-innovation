package com.truenins.voiddomain.client;

import com.truenins.voiddomain.core.DomainVolume;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class VoidDomain {

    private static UnlimitedVoid field;

    private VoidDomain() {}

    public static void open(Vec3 center, Vec3 look, float radius, int remainingTicks) {
        field = new UnlimitedVoid(Minecraft.getInstance(), center, look, radius,
            ClientEffectClock.ticks(), Math.max(1, remainingTicks));
    }

    public static void reschedule(int remainingTicks) {
        if (field != null) field.reschedule(ClientEffectClock.after(remainingTicks));
    }

    public static void close() {
        if (field != null) field.close(ClientEffectClock.ticks());
    }

    public static void clear() {
        if (field != null) {
            field.stop();
            field = null;
        }
    }

    public static UnlimitedVoid active() {
        return field;
    }

    public static boolean interior(UnlimitedVoid domain, Vec3 eye, long tick, float partial) {
        return DomainVolume.contains(domain.center, domain.radius * domain.expansion(tick, partial), eye);
    }
}
