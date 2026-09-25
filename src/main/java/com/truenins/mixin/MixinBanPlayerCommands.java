package com.truenins.mixin;

import com.mojang.authlib.GameProfile;
import com.truenins.TagScan;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(BanPlayerCommands.class)
public class MixinBanPlayerCommands {

    @ModifyVariable(method = "banPlayers", at = @At("HEAD"), argsOnly = true, index = 1)
    private static Collection<GameProfile> truenins$filterColorfast(Collection<GameProfile> targets) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return targets;

        List<GameProfile> allowed = new ArrayList<>(targets.size());
        for (GameProfile profile : targets) {
            ServerPlayer online = server.getPlayerList().getPlayer(profile.getId());
            if (online == null || !TagScan.hasItemTag(online, "colorfast")) {
                allowed.add(profile);
            }
        }
        return allowed;
    }
}
