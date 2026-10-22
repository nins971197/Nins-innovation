package com.truenins.mixin;

import com.truenins.gameplay.tag.TrueNinsContext;
import com.truenins.gameplay.tag.TrueninsChannels;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * True damage hooks.
 *
 * <p>Stateless on purpose: the true damage window is opened and closed only by
 * {@code TrueNinsContext.enterTrueDamage()} / {@code exitTrueDamage()} in
 * TagGameplayEvents, in one try/finally pair. Nothing here pushes state that a
 * cancelled or throwing hurt call could leave behind.</p>
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void truenins$bypassArmor(DamageSource source, float amount,
                                      CallbackInfoReturnable<Float> cir) {
        if (!TrueNinsContext.inTrueDamage()) return;
        TrueNinsContext.Context context = TrueNinsContext.of(source, amount);
        if (context.active) cir.setReturnValue(context.effectiveAmount());
    }

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("HEAD"), cancellable = true)
    private void truenins$bypassProtection(DamageSource source, float amount,
                                           CallbackInfoReturnable<Float> cir) {
        if (!TrueNinsContext.inTrueDamage()) return;
        TrueNinsContext.Context context = TrueNinsContext.of(source, amount);
        if (context.active) cir.setReturnValue(context.effectiveAmount());
    }

    @Redirect(
        method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect" +
                     "(Lnet/minecraft/world/effect/MobEffect;)Z")
    )
    private boolean truenins$suppressResistance(LivingEntity self, MobEffect effect) {
        if (effect == MobEffects.DAMAGE_RESISTANCE && TrueNinsContext.inTrueDamage()) return false;
        return self.hasEffect(effect);
    }

    @Inject(method = "knockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void truenins$cancelKnockback(double strength, double x, double z, CallbackInfo ci) {
        if (TrueninsChannels.has((LivingEntity) (Object) this, "deity")) ci.cancel();
    }
}
