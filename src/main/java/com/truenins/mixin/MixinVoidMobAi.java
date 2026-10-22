package com.truenins.mixin;

import com.truenins.spell.domain.VoidControl;
import com.truenins.spell.domain.JudgmentControl;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MixinVoidMobAi {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void truenins$pauseAi(CallbackInfo ci) {
        Mob mob = (Mob)(Object)this;
        // Registry backed: one batch sweep per tick instead of a radius query per mob per tick.
        if (VoidControl.isSuppressed(mob) || JudgmentControl.isSuppressed(mob)) ci.cancel();
    }
}
