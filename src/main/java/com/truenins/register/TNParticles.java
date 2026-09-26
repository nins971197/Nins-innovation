package com.truenins.register;

import com.truenins.TrueNinsMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TNParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
        DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, TrueNinsMod.MODID);

    public static final RegistryObject<SimpleParticleType> MOTE_SHARD =
        PARTICLES.register("mote_shard", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> MOTE_SPARK =
        PARTICLES.register("mote_spark", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> MOTE_VEIL =
        PARTICLES.register("mote_veil", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> MOTE_STAR =
        PARTICLES.register("mote_star", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> MOTE_CROSS =
        PARTICLES.register("mote_cross", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> MOTE_CROSS_SMALL =
        PARTICLES.register("mote_cross_small", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> FLASH_HALO =
        PARTICLES.register("flash_halo", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> FLASH_SLASH =
        PARTICLES.register("flash_slash", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> FLASH_SHARD =
        PARTICLES.register("flash_shard", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> MOTE_TIDE =
        PARTICLES.register("mote_tide", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> MOTE_EMBER =
        PARTICLES.register("mote_ember", () -> new SimpleParticleType(false));

    private TNParticles() {}
}
