package com.truenins;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class TagScan {

    private TagScan() {}

    public static boolean hasTag(ItemStack stack, String tag) {
        if (stack == null || stack.isEmpty()) return false;
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.getBoolean(tag);
    }

    public static boolean hasItemTag(Player player, String tag) {
        if (player == null) return false;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (hasTag(inventory.getItem(slot), tag)) return true;
        }
        return false;
    }
}
