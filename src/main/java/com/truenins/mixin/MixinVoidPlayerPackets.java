package com.truenins.mixin;

import com.truenins.spell.domain.VoidControl;
import com.truenins.spell.domain.JudgmentControl;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Enforce control on the server too, including clients without the input lock. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinVoidPlayerPackets {
    @Shadow public ServerPlayer player;

    @Shadow private boolean clientIsFloating;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void truenins$holdButAllowLook(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!player.server.isSameThread()) return;
        if (VoidControl.isSuppressed(player)) { clientIsFloating = false; ci.cancel(); return; }
        if (!JudgmentControl.isSuppressed(player)) return;
        float yaw = packet.getYRot(player.getYRot()), pitch = packet.getXRot(player.getXRot());
        if (Float.isFinite(yaw) && Float.isFinite(pitch)) {
            player.setYRot(net.minecraft.util.Mth.wrapDegrees(yaw));
            player.setXRot(net.minecraft.util.Mth.clamp(pitch, -90, 90));
            player.setYHeadRot(player.getYRot());
        }
        clientIsFloating = false;
        ci.cancel();
    }

    @Inject(method = {"handleMoveVehicle", "handlePlayerAction", "handleUseItem",
        "handleUseItemOn", "handleInteract", "handleContainerClick", "handleSetCarriedItem",
        "handleSetCreativeModeSlot", "handleAnimate", "handlePlayerCommand",
        "handleContainerButtonClick", "handlePlaceRecipe", "handlePickItem", "handleSetBeaconPacket",
        "handleRenameItem", "handleSelectTrade"},
        at = @At("HEAD"), cancellable = true)
    private void truenins$denyAction(CallbackInfo ci) {
        // Off-thread invocations must first reach vanilla's packet scheduling guard.
        if (player.server.isSameThread() && (VoidControl.isSuppressed(player) || JudgmentControl.isSuppressed(player))) ci.cancel();
    }
}
