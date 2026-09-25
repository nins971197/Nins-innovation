package com.truenins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.truenins.TrueNinsTags;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class TrueNinsCommand {

    private TrueNinsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx) {
        d.register(Commands.literal("truenins")
            .requires(s -> s.hasPermission(2))
            .then(Commands.literal("tag")
                .then(Commands.literal("apply")
                    .then(Commands.literal("trueme")
                        .executes(c -> applyTrueme(c, -1f))
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0f))
                            .executes(c -> applyTrueme(c, FloatArgumentType.getFloat(c, "amount")))))
                    .then(Commands.literal("deity").executes(c -> applyArmorTag(c, "deity")))
                    .then(Commands.literal("fly").executes(c -> applyArmorTag(c, "fly")))
                    .then(Commands.literal("immunity").executes(c -> applyArmorTag(c, "immunity")))
                    .then(Commands.literal("thorns").executes(c -> applyArmorTag(c, "thorns")))
                    .then(Commands.literal("adaptation").executes(c -> applyArmorTag(c, "adaptation")))
                    .then(Commands.literal("mitigation").executes(c -> applyArmorTag(c, "mitigation")))
                    .then(Commands.literal("revive").executes(c -> applyArmorTag(c, "revive")))
                    .then(Commands.literal("muryokusho").executes(c -> applyWeaponTag(c, "muryokusho")))
                    .then(Commands.literal("antiheal").executes(c -> applyWeaponTag(c, "antiheal")))
                    .then(Commands.literal("justice").executes(c -> applyWeaponTag(c, "justice")))
                    .then(Commands.literal("scaling").executes(c -> applyWeaponTag(c, "scaling")))
                    .then(Commands.literal("malice").executes(c -> applyWeaponTag(c, "malice")))
                    .then(Commands.literal("resolve").executes(c -> applyArmorTag(c, "resolve")))
                    .then(Commands.literal("siphon").executes(c -> applyWeaponTag(c, "siphon")))
                    .then(Commands.literal("rage").executes(c -> applyWeaponTag(c, "rage")))
                    .then(Commands.literal("unclear").executes(c -> applyWeaponTag(c, "unclear")))
                    .then(Commands.literal("colorfast").executes(c -> applyWeaponTag(c, "colorfast")))
                    .then(Commands.literal("blackflash")
                        .executes(c -> applyChance(c, "blackflash", 1))
                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                            .executes(c -> applyChance(c, "blackflash",
                                IntegerArgumentType.getInteger(c, "percent")))))
                    .then(Commands.literal("infinity").executes(c -> applyArmorTag(c, "infinity"))))
                .then(Commands.literal("remove").executes(TrueNinsCommand::remove)))
            .then(Commands.literal("check").executes(TrueNinsCommand::check))
        );
    }

    private static ChatFormatting colorOf(String tagName) {
        TrueNinsTags.Def def = TrueNinsTags.byKey(tagName);
        return def != null ? def.color() : ChatFormatting.DARK_RED;
    }

    private static int applyTrueme(CommandContext<CommandSourceStack> ctx, float amt) {
        Player player = (Player) ctx.getSource().getEntity();
        if (player == null) { ctx.getSource().sendFailure(Component.literal("Player only.")); return 0; }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { ctx.getSource().sendFailure(Component.literal("Hold an item.")); return 0; }
        CompoundTag tag = held.getOrCreateTag();
        if (amt >= 0f) tag.putFloat("true_me", amt);
        else tag.putByte("true_me", (byte) 1);
        ctx.getSource().sendSuccess(() -> Component.literal(
            "§6Applied §ftrueme" + (amt >= 0f ? " §7(" + String.format("%.1f", amt) + ")" : "")), true);
        return 1;
    }

    private static int applyArmorTag(CommandContext<CommandSourceStack> ctx, String tagName) {
        Player player = (Player) ctx.getSource().getEntity();
        if (player == null) { ctx.getSource().sendFailure(Component.literal("Player only.")); return 0; }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { ctx.getSource().sendFailure(Component.literal("Hold an item.")); return 0; }
        held.getOrCreateTag().putByte(tagName, (byte) 1);
        MutableComponent feedback = Component.literal("Applied ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(tagName).withStyle(colorOf(tagName)));
        ctx.getSource().sendSuccess(() -> feedback, true);
        return 1;
    }

    private static int applyWeaponTag(CommandContext<CommandSourceStack> ctx, String tagName) {
        Player player = (Player) ctx.getSource().getEntity();
        if (player == null) { ctx.getSource().sendFailure(Component.literal("Player only.")); return 0; }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { ctx.getSource().sendFailure(Component.literal("Hold an item.")); return 0; }
        held.getOrCreateTag().putByte(tagName, (byte) 1);
        MutableComponent feedback = Component.literal("Applied ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(tagName).withStyle(colorOf(tagName)));
        ctx.getSource().sendSuccess(() -> feedback, true);
        return 1;
    }

    private static int applyChance(CommandContext<CommandSourceStack> ctx, String tagName, int percent) {
        Player player = (Player) ctx.getSource().getEntity();
        if (player == null) { ctx.getSource().sendFailure(Component.literal("Player only.")); return 0; }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { ctx.getSource().sendFailure(Component.literal("Hold an item.")); return 0; }
        held.getOrCreateTag().putInt(tagName, percent);
        MutableComponent feedback = Component.literal("Applied ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(tagName + " " + percent + "%").withStyle(colorOf(tagName)));
        ctx.getSource().sendSuccess(() -> feedback, true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        Player player = (Player) ctx.getSource().getEntity();
        if (player == null) { ctx.getSource().sendFailure(Component.literal("Player only.")); return 0; }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { ctx.getSource().sendFailure(Component.literal("Hold an item.")); return 0; }
        CompoundTag tag = held.getTag();
        if (tag != null) {
            for (String t : TrueNinsTags.REMOVABLE) tag.remove(t);
            if (tag.isEmpty()) held.setTag(null);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Removed all TrueNins tags")
            .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int check(CommandContext<CommandSourceStack> ctx) {
        Player player = (Player) ctx.getSource().getEntity();
        if (player == null) { ctx.getSource().sendFailure(Component.literal("Player only.")); return 0; }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { ctx.getSource().sendFailure(Component.literal("Hold an item.")); return 0; }
        CompoundTag tag = held.getTag();

        boolean has = false;
        if (tag != null) {
            for (String t : TrueNinsTags.REMOVABLE) {
                if (tag.contains(t)) { has = true; break; }
            }
        }

        if (!has) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                "No tags. Use /truenins tag apply <" + TrueNinsTags.COMMAND_LIST + ">")
                .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }

        MutableComponent out = Component.literal("══ TrueNins ══").withStyle(ChatFormatting.GOLD);
        if (tag.contains("true_me")) {
            float d = tag.getFloat("true_me");
            out.append(Component.literal("\n  trueme: " + (d > 1f ? String.format("%.1f", d) : "weapon base"))
                .withStyle(ChatFormatting.GOLD));
        }
        for (TrueNinsTags.Def def : TrueNinsTags.ALL) {
            if (def.key().equals("true_me")) continue;
            if (tag.contains(def.key())) {
                String suffix = def.key().equals("blackflash") ? " §7(" + tag.getInt("blackflash") + "%)" : "";
                out.append(Component.literal("\n  " + def.key() + suffix).withStyle(def.color()));
            }
        }
        ctx.getSource().sendSuccess(() -> out, false);
        return 1;
    }
}
