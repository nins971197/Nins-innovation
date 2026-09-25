package com.truenins.event;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsEnchantments;
import com.truenins.TrueNinsMod;
import com.truenins.TrueNinsTags;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            appendTagLines(event, tag);
        }
        if (TrueNinsConfig.enchantTooltipDescriptions()) {
            appendEnchantmentDescriptions(event, stack);
        }
    }

    private static void appendTagLines(ItemTooltipEvent event, CompoundTag tag) {
        for (TrueNinsTags.Def def : TrueNinsTags.ALL) {
            if (!tag.contains(def.key())) continue;

            MutableComponent line = Component.literal(" " + def.icon() + " " + def.command())
                .withStyle(def.color());

            if (def.key().equals("true_me")) {
                float damage = tag.getFloat("true_me");
                if (damage > 1f) {
                    line.append(Component.literal(" (" + String.format("%.1f", damage) + ")")
                        .withStyle(ChatFormatting.GRAY));
                }
            }
            event.getToolTip().add(line);
        }
    }

    private static void appendEnchantmentDescriptions(ItemTooltipEvent event, ItemStack stack) {
        if (stack.isEmpty()) return;
        for (TrueNinsEnchantments.Def def : TrueNinsEnchantments.DEFS) {
            if (TrueNinsEnchantments.levelOf(stack, def.tagKey()) <= 0) continue;

            MutableComponent line = Component.literal(" \u25B8 ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable("enchantment.truenins." + def.id())
                    .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" \u2014 ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("enchantment.truenins." + def.id() + ".desc")
                    .withStyle(ChatFormatting.DARK_GRAY));
            event.getToolTip().add(line);
        }
    }
}
