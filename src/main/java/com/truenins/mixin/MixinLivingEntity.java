package com.truenins.mixin;

import com.truenins.TrueNinsContext;
import com.truenins.TrueNinsEnchantments;
import com.truenins.TrueNinsMod;
import com.truenins.ReviveHandler;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Inject(method = "hurt", at = @At("HEAD"))
    private void truenins$onHurtEnter(DamageSource source, float amount,
                                      CallbackInfoReturnable<Boolean> cir) {
        TrueNinsContext.Context ctx = computeContext(source, amount);
        TrueNinsContext.push(ctx);
        if (ctx.active) {
            LivingEntity self = (LivingEntity)(Object)this;
            ctx.preDamageHealth = self.getHealth();
            ctx.preAbsorption = self.getAbsorptionAmount();
            self.invulnerableTime = 0;
            self.hurtTime = 0;
        }
    }

    @Unique
    private static TrueNinsContext.Context computeContext(DamageSource source, float rawAmount) {
        if (!(source.getEntity() instanceof LivingEntity attacker))
            return new TrueNinsContext.Context(false, rawAmount, null);
        ItemStack w = attacker.getMainHandItem();
        if (w.isEmpty()) return new TrueNinsContext.Context(false, rawAmount, null);

        Float d = TrueNinsContext.readTrueMeAmount(w);
        if (d != null) return new TrueNinsContext.Context(true, rawAmount, d >= 0f ? d : null);
        return new TrueNinsContext.Context(false, rawAmount, null);
    }

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void truenins$bypassArmor(DamageSource source, float amount,
                                      CallbackInfoReturnable<Float> cir) {
        if (TrueNinsContext.isActive()) {
            TrueNinsContext.Context ctx = TrueNinsContext.peek();
            if (ctx != null) cir.setReturnValue(ctx.effectiveAmount());
        }
    }

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("HEAD"), cancellable = true)
    private void truenins$bypassProtection(DamageSource source, float amount,
                                           CallbackInfoReturnable<Float> cir) {
        if (TrueNinsContext.isActive()) {
            TrueNinsContext.Context ctx = TrueNinsContext.peek();
            if (ctx != null) cir.setReturnValue(ctx.effectiveAmount());
        }
    }

    @Redirect(
        method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect" +
                     "(Lnet/minecraft/world/effect/MobEffect;)Z")
    )
    private boolean truenins$suppressResistance(LivingEntity self, MobEffect e) {
        if (e == MobEffects.DAMAGE_RESISTANCE && TrueNinsContext.isActive())
            return false;
        return self.hasEffect(e);
    }

    @Unique
    private static final java.lang.reflect.Method DIE_METHOD = getDieMethod();

    @Unique
    private static java.lang.reflect.Method getDieMethod() {
        for (String name : new String[]{"m_6667_", "die"}) {
            try {
                java.lang.reflect.Method m = LivingEntity.class
                    .getDeclaredMethod(name, DamageSource.class);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
        }
        throw new RuntimeException("TrueNins: cannot find LivingEntity.die()");
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void truenins$onHurtExit(DamageSource source, float amount,
                                     CallbackInfoReturnable<Boolean> cir) {
        TrueNinsContext.Context ctx = TrueNinsContext.peek();
        if (ctx != null && ctx.active) {
            LivingEntity self = (LivingEntity)(Object)this;
            float expected = ctx.preDamageHealth - ctx.effectiveAmount();

            for (int round = 0; round < 20; round++) {
                float actual = self.getHealth();
                if (actual <= Math.max(0.0F, expected) || actual <= 0.0F || self.deathTime > 0)
                    break;
                self.setHealth(Math.max(0.0F, expected));
            }

            if (ctx.effectiveAmount() > ctx.preDamageHealth
                && self.getHealth() > 0.0F
                && self.deathTime == 0) {
                self.setHealth(0.0F);
                try { DIE_METHOD.invoke(self, source); } catch (Exception ignored) {}
            }
        }
        TrueNinsContext.pop();
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void truenins$reviveOnDie(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof Player player)) return;
        if (self.level().isClientSide) return;
        if (player.isCreative() || player.isSpectator()) return;

        if (TrueNinsMod.hasTag(player, "deity")) return;
        if (!ReviveHandler.hasRevive(player)) return;

        long now = player.level().getGameTime();
        int channel = TrueNinsEnchantments.bestLevel(player, "revive") > 0 ? 1 : 0;
        if (ReviveHandler.isOnCooldown(player, now, channel)) return;

        ci.cancel();
        ReviveHandler.execute(player, channel == 1 ? TrueNinsEnchantments.bestLevel(player, "revive") : 0);
        ReviveHandler.setCooldown(player, now, channel);
    }

    @Inject(method = "knockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void truenins$cancelKnockback(double x, double y, double z, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (TrueNinsMod.hasTag(self, "deity")) ci.cancel();
    }
}
