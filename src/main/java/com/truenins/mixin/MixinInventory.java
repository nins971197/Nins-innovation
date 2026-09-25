package com.truenins.mixin;

import com.truenins.TagScan;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;

@Mixin(Inventory.class)
public abstract class MixinInventory {

    @ModifyVariable(
        method = "clearOrCountMatchingItems(Ljava/util/function/Predicate;ILnet/minecraft/world/Container;)I",
        at = @At("HEAD"),
        argsOnly = true,
        index = 1
    )
    private Predicate<ItemStack> truenins$protectUnclear(Predicate<ItemStack> original) {

        if (original == null) return stack -> !TagScan.hasTag(stack, "unclear");
        return stack -> !TagScan.hasTag(stack, "unclear") && original.test(stack);
    }
}
