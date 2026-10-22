package com.truenins.mixin;

import com.truenins.spell.domain.VoidControl;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinVoidTurn {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void truenins$lockCamera(double yaw, double pitch, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if ((Object)this == player && player != null && VoidControl.isAffected(player)) ci.cancel();
    }
}
