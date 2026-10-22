package com.truenins.mixin;

import com.truenins.gameplay.tag.TrueninsChannels;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Deity keyword: health may never be lowered, and death is blocked in the events.
 * Uses the shared channel cache instead of its own equipment scan.
 */
@Mixin(LivingEntity.class)
public class MixinDeityProtection {

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void truenins$protectDeity(float newHealth, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (newHealth >= self.getHealth() && newHealth > 0.0F) return;
        if (TrueninsChannels.has(self, "deity")) ci.cancel();
    }
}
