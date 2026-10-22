package com.truenins.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.truenins.TrueNinsMod;
import com.truenins.client.orb.OrbVfxRenderer;
import com.truenins.client.domain.shingan.ShinganSceneEffect;
import com.truenins.voiddomain.client.EffectShader;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

/** Reloaded with the resource pack; the sky must not fade with terrain fog. */
@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PixelShaders {
    private static ShaderInstance sky;
    private static ShaderInstance volume;
    private PixelShaders() {}
    public static ShaderInstance sky() { return sky; }
    public static ShaderInstance volume() { return volume; }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new EffectShader(event.getResourceProvider(),
            new ResourceLocation(TrueNinsMod.MODID, "conflagration_sky"), DefaultVertexFormat.POSITION_TEX_COLOR),
            com.truenins.client.conflagration.ConflagrationScene::setSky);
        event.registerShader(new EffectShader(event.getResourceProvider(),
            new ResourceLocation(TrueNinsMod.MODID, "conflagration_meteors"), DefaultVertexFormat.POSITION_TEX_COLOR),
            com.truenins.client.conflagration.ConflagrationScene::setMeteors);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
            new ResourceLocation(TrueNinsMod.MODID, "pixel_sky"), DefaultVertexFormat.NEW_ENTITY), shader -> sky = shader);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
            new ResourceLocation(TrueNinsMod.MODID, "pixel_volume"), DefaultVertexFormat.NEW_ENTITY), shader -> volume = shader);
        event.registerShader(new EffectShader(event.getResourceProvider(),
            new ResourceLocation(TrueNinsMod.MODID, "orb_volume"), DefaultVertexFormat.POSITION_TEX_COLOR),
            OrbVfxRenderer::setBlue);
        event.registerShader(new EffectShader(event.getResourceProvider(),
            new ResourceLocation(TrueNinsMod.MODID, "orb_red"), DefaultVertexFormat.POSITION_TEX_COLOR),
            OrbVfxRenderer::setRed);
        event.registerShader(new EffectShader(event.getResourceProvider(),
            new ResourceLocation(TrueNinsMod.MODID, "orb_purple"), DefaultVertexFormat.POSITION_TEX_COLOR),
            OrbVfxRenderer::setPurple);
        event.registerShader(new EffectShader(event.getResourceProvider(),
            new ResourceLocation(TrueNinsMod.MODID, "shingan_scene"), DefaultVertexFormat.POSITION_TEX_COLOR),
            ShinganSceneEffect::setShader);
        event.registerShader(new EffectShader(event.getResourceProvider(),
            new ResourceLocation(TrueNinsMod.MODID, "judgment_scene"), DefaultVertexFormat.POSITION_TEX_COLOR),
            com.truenins.client.domain.judgment.JudgmentSceneEffect::setShader);
        event.registerShader(new EffectShader(event.getResourceProvider(),
            new ResourceLocation(TrueNinsMod.MODID, "executioner_sword"), DefaultVertexFormat.POSITION_TEX_COLOR),
            com.truenins.client.domain.judgment.ExecutionerSwordEffect::setShader);
    }
}
