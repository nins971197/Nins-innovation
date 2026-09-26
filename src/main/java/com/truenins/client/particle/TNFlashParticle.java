package com.truenins.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class TNFlashParticle extends TextureSheetParticle {

    public record Look(float sizeFrom, float sizeTo, float spin, float rollSpread,
                       float peakAlpha, int life, float growPower, float hold, float friction) {}

    public static final Look LOOK_CROSS =
        new Look(0.72F, 1.10F, 0.0F, 0.0F, 1.00F, 6, 0.20F, 0.34F, 0.90F);
    public static final Look LOOK_HALO =
        new Look(0.65F, 1.30F, 0.0F, 0.0F, 0.50F, 7, 0.25F, 0.30F, 0.90F);
    public static final Look LOOK_SHARD =
        new Look(0.17F, 0.33F, 0.02F, 3.15F, 1.00F, 9, 0.30F, 0.10F, 0.86F);
    public static final Look LOOK_SLASH =
        new Look(0.90F, 2.90F, 0.000F, 3.15F, 0.95F, 7, 0.30F, 0.20F, 0.90F);

    private final SpriteSet sprites;
    private final Look look;

    protected TNFlashParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz, SpriteSet sprites, Look look) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.look = look;
        this.lifetime = look.life();
        this.friction = look.friction();
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.roll = (this.random.nextFloat() - 0.5F) * 2.0F * look.rollSpread();
        this.oRoll = this.roll;
        this.quadSize = look.sizeFrom();
        this.setSprite(sprites.get(0, Math.max(1, look.life() - 1)));        this.setColor(1.0F, 1.0F, 1.0F);
        this.setAlpha(look.peakAlpha());
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick();
        this.roll += this.look.spin();
        if (this.removed) return;

        int frames = Math.max(1, this.lifetime - 1);
        this.setSprite(this.sprites.get(Mth.clamp(this.age, 0, frames), frames));

        float t = Mth.clamp((float) this.age / (float) this.lifetime, 0.0F, 1.0F);
        float env;
        if (t <= this.look.hold()) {
            env = 1.0F;
        } else {
            float k = (1.0F - t) / Math.max(1.0E-4F, 1.0F - this.look.hold());
            env = (float) Math.pow(Mth.clamp(k, 0.0F, 1.0F), 1.35D);
        }
        this.setAlpha(this.look.peakAlpha() * env);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float t = Mth.clamp(((float) this.age + partialTicks) / (float) this.lifetime, 0.0F, 1.0F);
        float e = (float) Math.pow(t, this.look.growPower());
        return Mth.lerp(e, this.look.sizeFrom(), this.look.sizeTo());
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        double px = Mth.lerp(partialTicks, this.xo, this.x);
        double py = Mth.lerp(partialTicks, this.yo, this.y);
        double pz = Mth.lerp(partialTicks, this.zo, this.z);
        double dx = camPos.x - px;
        double dy = camPos.y - py;
        double dz = camPos.z - pz;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4D) {
            super.render(buffer, camera, partialTicks);
            return;
        }
        double push = Math.min(1.25D, len * 0.30D);
        double sx = dx / len * push;
        double sy = dy / len * push;
        double sz = dz / len * push;
        this.x += sx; this.xo += sx;
        this.y += sy; this.yo += sy;
        this.z += sz; this.zo += sz;
        super.render(buffer, camera, partialTicks);
        this.x -= sx; this.xo -= sx;
        this.y -= sy; this.yo -= sy;
        this.z -= sz; this.zo -= sz;
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
            return new TNFlashParticle(level, x, y, z, vx, vy, vz, this.sprites, this.look);
        }
    }
}
