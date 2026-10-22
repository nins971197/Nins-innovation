package com.truenins.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The replacement cloud layers belong to the scene shader; do not draw white clouds over them. */
@Mixin(LevelRenderer.class)
public abstract class MixinConflagrationClouds {
    @Inject(method="renderClouds",at=@At("HEAD"),cancellable=true)
    private void truenins$conflagrationClouds(CallbackInfo ci) {
        if(com.truenins.client.conflagration.ConflagrationScene.rendering())ci.cancel();
    }
}
