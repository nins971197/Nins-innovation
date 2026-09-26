package com.truenins.register;

import com.truenins.TrueNinsMod;
import com.truenins.entity.BlueOrbEntity;
import com.truenins.entity.OrbShardEntity;
import com.truenins.entity.RedOrbEntity;
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
            .sized(1.4F, 1.4F)
            .clientTrackingRange(16)
            .updateInterval(1)
            .build(new ResourceLocation(TrueNinsMod.MODID, "blue_orb").toString()));

    public static final RegistryObject<EntityType<RedOrbEntity>> RED_ORB =
        ENTITIES.register("red_orb", () -> EntityType.Builder
            .<RedOrbEntity>of(RedOrbEntity::new, MobCategory.MISC)
            .sized(1.6F, 1.6F)
            .clientTrackingRange(16)
            .updateInterval(1)
            .build(new ResourceLocation(TrueNinsMod.MODID, "red_orb").toString()));

    public static final RegistryObject<EntityType<OrbShardEntity>> ORB_SHARD =
        ENTITIES.register("orb_shard", () -> EntityType.Builder
            .<OrbShardEntity>of(OrbShardEntity::new, MobCategory.MISC)
            .sized(0.6F, 0.6F)
            .clientTrackingRange(12)
            .updateInterval(1)
            .noSave()
            .build(new ResourceLocation(TrueNinsMod.MODID, "orb_shard").toString()));

    private TNEntities() {}
}
