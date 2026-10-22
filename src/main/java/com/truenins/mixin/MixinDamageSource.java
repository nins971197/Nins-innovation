package com.truenins.mixin;

import com.truenins.gameplay.damage.TrueDamageSource;
import com.truenins.gameplay.tag.TrueNinsContext;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grants the vanilla bypass tags while a true damage hit is being applied.
 *
 * <p>The window is the thread local entered by TagGameplayEvents around the single
 * nested {@code hurt} call, so every other damage source on that thread keeps its
 * normal immunity and bypass behaviour.</p>
 */
@Mixin(DamageSource.class)
public class MixinDamageSource {

    @Inject(method = "is", at = @At("HEAD"), cancellable = true)
    private void truenins$addBypassTags(TagKey<DamageType> tag,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!TrueNinsContext.inTrueDamage()) return;
        // The window alone would leak the bypass tags into damage nested inside it (thorns
        // retaliation, recoil and similar). Only the dedicated true damage source may bypass.
        if (!((Object) this instanceof TrueDamageSource)) return;

        String path = tag.location().getPath();
        if (path.startsWith("bypasses_") || path.equals("always_hurts_ender_dragons")) {
            cir.setReturnValue(true);
        }
    }
}
