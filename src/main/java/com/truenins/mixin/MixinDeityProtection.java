package com.truenins.mixin;

import com.truenins.TrueNinsEnchantments;
import com.truenins.TrueNinsMod;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinDeityProtection {

    @Unique
    private static boolean hasDeity(LivingEntity e) {
        if (e.getTags().contains("deity")) return true;
        if (e.getPersistentData().getBoolean("deity")) return true;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
                ItemStack s = e.getItemBySlot(slot);
                if (s.hasTag() && s.getTag().getBoolean("deity")) return true;
                if (TrueNinsEnchantments.levelOf(s, "deity") > 0) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void truenins$protectDeity(float newHealth, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!hasDeity(self)) return;
        float current = self.getHealth();
        if (newHealth < current || newHealth <= 0.0F) ci.cancel();
    }
}
