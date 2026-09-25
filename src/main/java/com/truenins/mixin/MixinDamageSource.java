package com.truenins.mixin;

import com.truenins.TrueNinsContext;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageSource.class)
public class MixinDamageSource {

    @Inject(method = "is", at = @At("HEAD"), cancellable = true)
    private void truenins$addBypassTags(TagKey<DamageType> tag,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!TrueNinsContext.isTrueMeAttack((DamageSource) (Object) this)) return;

        String path = tag.location().getPath();
        if (path.startsWith("bypasses_") || path.equals("always_hurts_ender_dragons")) {
            cir.setReturnValue(true);
        }
    }
}
