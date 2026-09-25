package com.truenins.mixin;

import com.truenins.AntihealTracker;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinAntiheal {

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    public void truenins$antihealHeal(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        float r = AntihealTracker.getReduction(self);
        if (r <= 0f) return;

        ci.cancel();
        AntihealTracker.FROM_HEAL.set(true);
        try {
            float reduced = amount * (1f - r);
            if (reduced > 0f) self.setHealth(self.getHealth() + reduced);
        } finally {
            AntihealTracker.FROM_HEAL.set(false);
        }
    }

    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
    private float truenins$antihealReduce(float newHealth) {
        if (AntihealTracker.FROM_HEAL.get()) return newHealth;

        LivingEntity self = (LivingEntity)(Object)this;
        if (self.level().isClientSide) return newHealth;

        float cur = self.getHealth();
        if (newHealth <= cur) return newHealth;

        float r = AntihealTracker.getReduction(self);
        if (r <= 0f) return newHealth;
        if (r >= 1.0f) return cur;

        return cur + (newHealth - cur) * (1f - r);
    }
}
