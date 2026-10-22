package com.truenins.voiddomain.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.truenins.TrueNinsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class VoidDomainShaders {

    private VoidDomainShaders() {}

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        ResourceProvider provider = event.getResourceProvider();
        event.registerShader(
            new EffectShader(provider, id("unlimited_void"), DefaultVertexFormat.POSITION_TEX_COLOR),
            UnlimitedVoidRenderer::setShader);
        event.registerShader(
            new EffectShader(provider, id("void_terrain_clear"), DefaultVertexFormat.POSITION_TEX_COLOR),
            DomainWorldCapture::setShader);
        event.registerShader(
            new EffectShader(provider, id("void_stream"), DefaultVertexFormat.POSITION_TEX_COLOR),
            VoidStreamRenderer::setShader);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(TrueNinsMod.MODID, path);
    }
}
