package com.truenins.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class TNMoteParticle extends TextureSheetParticle {

    public record Look(float r0, float g0, float b0, float r1, float g1, float b1,
                       float peakAlpha, float size, float spin, float rise, float drag) {}

    public static final Look LOOK_SHARD =
        new Look(0.22F, 0.36F, 0.98F, 0.42F, 0.64F, 1.00F, 0.85F, 0.16F, 0.055F, 0.0040F, 0.95F);

    public static final Look LOOK_SPARK =
        new Look(0.55F, 0.86F, 1.00F, 0.92F, 0.98F, 1.00F, 0.95F, 0.20F, 0.030F, 0.0060F, 0.96F);

    public static final Look LOOK_VEIL =
        new Look(0.62F, 0.45F, 1.00F, 0.82F, 0.64F, 1.00F, 0.30F, 0.55F, -0.020F, 0.0030F, 0.94F);

    public static final Look LOOK_STAR =
        new Look(0.80F, 0.90F, 1.00F, 1.00F, 1.00F, 1.00F, 1.00F, 0.22F, 0.120F, 0.0080F, 0.97F);

    public static final Look LOOK_TIDE =
        new Look(0.42F, 0.72F, 1.00F, 0.80F, 0.94F, 1.00F, 0.85F, 0.34F, -0.012F, 0.0035F, 0.95F);

    public static final Look LOOK_EMBER =
        new Look(0.96F, 0.20F, 0.09F, 1.00F, 0.62F, 0.22F, 0.90F, 0.30F, 0.030F, 0.0055F, 0.94F);

    private final Look look;

    protected TNMoteParticle(ClientLevel level, double x, double y, double z,
                             double vx, double vy, double vz, SpriteSet sprites, Look look) {
        super(level, x, y, z, vx, vy, vz);
        this.look = look;
        this.lifetime = 26 + this.random.nextInt(26);
        this.friction = look.drag();
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.roll = this.random.nextFloat() * 6.2831855F;
        this.oRoll = this.roll;
        this.quadSize = look.size() * (0.75F + this.random.nextFloat() * 0.5F);
        this.pickSprite(sprites);
        this.setColor(look.r0(), look.g0(), look.b0());
        this.setAlpha(0.0F);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        this.roll += this.look.spin();
        if (this.removed) return;

        float t = envelope();
        this.setAlpha(this.look.peakAlpha() * t);
        this.setColor(
            Mth.lerp(t, this.look.r0(), this.look.r1()),
            Mth.lerp(t, this.look.g0(), this.look.g1()),
            Mth.lerp(t, this.look.b0(), this.look.b1()));
        this.yd += this.look.rise();
    }

    private float envelope() {
        float t = (float) this.age / (float) this.lifetime;
        float e = t < 0.22F ? t / 0.22F : (1.0F - t) / 0.78F;
        return Mth.clamp(e, 0.0F, 1.0F);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float t = (float) this.age / (float) this.lifetime;
        float e = t < 0.22F ? t / 0.22F : (1.0F - t) / 0.78F;
        return this.quadSize * (0.55F + 0.45F * Mth.clamp(e, 0.0F, 1.0F));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0x00F000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;
        private final Look look;

        public Provider(SpriteSet sprites, Look look) {
            this.sprites = sprites;
            this.look = look;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new TNMoteParticle(level, x, y, z, vx, vy, vz, this.sprites, this.look);
        }
    }
}
