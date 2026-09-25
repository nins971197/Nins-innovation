package com.truenins.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinEntityTagSync {

    private static final String[] TAGS = {
        "deity", "fly", "immunity", "thorns", "adaptation", "mitigation", "revive"
    };

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void truenins$syncTags(CompoundTag root, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        for (String t : TAGS) {
            if (root.getBoolean(t)) {
                self.getPersistentData().putBoolean(t, true);
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void truenins$saveTags(CompoundTag root, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        for (String t : TAGS) {
            if (self.getPersistentData().getBoolean(t)) {
                root.putBoolean(t, true);
            }
        }
    }
}
