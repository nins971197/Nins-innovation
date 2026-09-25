package com.truenins.mixin;

import com.truenins.TagScan;
import net.minecraft.server.commands.KickCommand;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(KickCommand.class)
public class MixinKickCommand {

    @ModifyVariable(method = "kickPlayers", at = @At("HEAD"), argsOnly = true, index = 1)
    private static Collection<ServerPlayer> truenins$filterColorfast(Collection<ServerPlayer> targets) {
        List<ServerPlayer> allowed = new ArrayList<>(targets.size());
        for (ServerPlayer player : targets) {
            if (!TagScan.hasItemTag(player, "colorfast")) {
                allowed.add(player);
            }
        }
        return allowed;
    }
}
