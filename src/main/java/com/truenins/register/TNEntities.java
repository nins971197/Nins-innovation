package com.truenins.register;

import com.truenins.TrueNinsMod;
import com.truenins.entity.orb.BlueOrbEntity;
import com.truenins.entity.orb.OrbShardEntity;
import com.truenins.entity.orb.RedOrbEntity;
import com.truenins.entity.orb.PurpleOrbEntity;
import com.truenins.entity.domain.ShinganDecorEntity;
import com.truenins.entity.domain.ShinganDomainEntity;
import com.truenins.entity.domain.VoidDomainEntity;
import com.truenins.entity.domain.JudgmentDomainEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TNEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TrueNinsMod.MODID);

    public static final RegistryObject<EntityType<BlueOrbEntity>> BLUE_ORB =
        ENTITIES.register("blue_orb", () -> EntityType.Builder
            .<BlueOrbEntity>of(BlueOrbEntity::new, MobCategory.MISC)
            .sized(0.75F, 0.75F)
            .clientTrackingRange(16)
            .updateInterval(1)
            .noSave()
            .build(new ResourceLocation(TrueNinsMod.MODID, "blue_orb").toString()));

    public static final RegistryObject<EntityType<RedOrbEntity>> RED_ORB =
        ENTITIES.register("red_orb", () -> EntityType.Builder
            .<RedOrbEntity>of(RedOrbEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(16)
            .updateInterval(1)
            .noSave()
            .build(new ResourceLocation(TrueNinsMod.MODID, "red_orb").toString()));

    public static final RegistryObject<EntityType<PurpleOrbEntity>> PURPLE_ORB =
        ENTITIES.register("purple_orb", () -> EntityType.Builder
            .<PurpleOrbEntity>of(PurpleOrbEntity::new, MobCategory.MISC)
            .sized(1.0F, 1.0F).clientTrackingRange(32).updateInterval(1).noSave().fireImmune()
            .build(new ResourceLocation(TrueNinsMod.MODID, "purple_orb").toString()));

    public static final RegistryObject<EntityType<OrbShardEntity>> ORB_SHARD =
        ENTITIES.register("orb_shard", () -> EntityType.Builder
            .<OrbShardEntity>of(OrbShardEntity::new, MobCategory.MISC)
            .sized(0.6F, 0.6F)
            .clientTrackingRange(12)
            .updateInterval(1)
            .noSave()
            .build(new ResourceLocation(TrueNinsMod.MODID, "orb_shard").toString()));

    public static final RegistryObject<EntityType<VoidDomainEntity>> VOID_DOMAIN =
        ENTITIES.register("void_domain", () -> EntityType.Builder
            .<VoidDomainEntity>of(VoidDomainEntity::new, MobCategory.MISC)
            .sized(0.1F, 0.1F)
            .clientTrackingRange(32)
            .updateInterval(1)
            .noSave()
            .fireImmune()
            .build(new ResourceLocation(TrueNinsMod.MODID, "void_domain").toString()));

    public static final RegistryObject<EntityType<ShinganDomainEntity>> SHINGAN_DOMAIN =
        ENTITIES.register("shingan_domain", () -> EntityType.Builder
            .<ShinganDomainEntity>of(ShinganDomainEntity::new, MobCategory.MISC)
            .sized(0.1F, 0.1F)
            .clientTrackingRange(32)
            .updateInterval(1)
            .noSave()
            .fireImmune()
            .build(new ResourceLocation(TrueNinsMod.MODID, "shingan_domain").toString()));

    public static final RegistryObject<EntityType<ShinganDecorEntity>> SHINGAN_DECOR =
        ENTITIES.register("shingan_decor", () -> EntityType.Builder
            .<ShinganDecorEntity>of(ShinganDecorEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.6F)
            .clientTrackingRange(16)
            .updateInterval(1)
            .noSave()
            .build(new ResourceLocation(TrueNinsMod.MODID, "shingan_decor").toString()));

    public static final RegistryObject<EntityType<JudgmentDomainEntity>> JUDGMENT_DOMAIN =
        ENTITIES.register("judgment_domain", () -> EntityType.Builder
            .<JudgmentDomainEntity>of(JudgmentDomainEntity::new, MobCategory.MISC)
            .sized(0.1F, 0.1F).clientTrackingRange(48).updateInterval(1).noSave().fireImmune()
            .build(new ResourceLocation(TrueNinsMod.MODID, "judgment_domain").toString()));

    public static final RegistryObject<EntityType<com.truenins.entity.ConflagrationEntity>> CONFLAGRATION =
        ENTITIES.register("eternal_conflagration", () -> EntityType.Builder
            .<com.truenins.entity.ConflagrationEntity>of(com.truenins.entity.ConflagrationEntity::new, MobCategory.MISC)
            .sized(.1f,.1f).clientTrackingRange(8).updateInterval(20).noSave().fireImmune()
            .build(new ResourceLocation(TrueNinsMod.MODID, "eternal_conflagration").toString()));

    private TNEntities() {}
}
