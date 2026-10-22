package com.truenins.client.domain.voiddomain;

import com.truenins.TrueNinsMod;
import com.truenins.entity.domain.VoidDomainEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class TNVoidDomainRenderer extends EntityRenderer<VoidDomainEntity> {

    private static final ResourceLocation EMPTY =
        new ResourceLocation(TrueNinsMod.MODID, "textures/entity/orb_glow.png");

    public TNVoidDomainRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidDomainEntity entity) {
        return EMPTY;
    }
}
