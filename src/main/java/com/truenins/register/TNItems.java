package com.truenins.register;

import com.truenins.TrueNinsMod;
import com.truenins.item.TagGuideItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TNItems {

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, TrueNinsMod.MODID);

    public static final RegistryObject<Item> TAG_GUIDE =
        ITEMS.register("tag_guide", () -> new TagGuideItem(new Item.Properties().stacksTo(1)));

    private TNItems() {}

    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TAG_GUIDE);
        }
    }
}
