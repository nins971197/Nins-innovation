package com.truenins.client.guide;

import net.minecraft.client.Minecraft;

public final class TagGuideClient {

    private TagGuideClient() {}

    public static void open() {
        Minecraft.getInstance().setScreen(new TagGuideScreen());
    }
}
