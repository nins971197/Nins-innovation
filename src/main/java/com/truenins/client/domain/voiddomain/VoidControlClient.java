package com.truenins.client.domain.voiddomain;

import com.truenins.TrueNinsMod;
import com.truenins.spell.domain.VoidControl;
import com.truenins.spell.domain.JudgmentControl;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT)
public final class VoidControlClient {
    private VoidControlClient() {}
    @SubscribeEvent public static void movement(MovementInputUpdateEvent event) {
        if (!VoidControl.isAffected(event.getEntity()) && !JudgmentControl.isAffected(event.getEntity())) return;
        var input = event.getInput();
        input.forwardImpulse = input.leftImpulse = 0;
        input.up = input.down = input.left = input.right = false;
        input.jumping = input.shiftKeyDown = false;
    }
    @SubscribeEvent public static void action(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (player != null && (VoidControl.isAffected(player) || JudgmentControl.isAffected(player))) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
