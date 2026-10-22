package com.truenins.voiddomain.client;

import com.truenins.TrueNinsMod;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

final class VoidDump {

    private static long lastTick = -1L;
    private static boolean failed;

    private VoidDump() {}

    static void maybeDump(long tick, Minecraft mc) {
        if (failed || !com.truenins.TrueNinsConfig.voidDebugFrames()
              || tick % 40 != 0L || tick == lastTick) return;
        lastTick = tick;

        try {
            File dir = new File(mc.gameDirectory, "void-dump");
            if (!dir.exists() && !dir.mkdirs()) return;

            int sceneW = SceneSnapshot.lastWidth;
            int sceneH = SceneSnapshot.lastHeight;
            if (sceneW > 0 && sceneH > 0) {
                write(dir, "scene", readTexture(WaterSceneCapture.color(), sceneW, sceneH), sceneW, sceneH);
            }
            writeFramebuffer(dir, mc);
            TrueNinsMod.LOGGER.info("[void] dumped frames to {}", dir.getAbsolutePath());
        } catch (Throwable t) {
            failed = true;
            TrueNinsMod.LOGGER.warn("[void] dump disabled: {}", t.toString());
        }
    }

    private static ByteBuffer readTexture(int textureId, int width, int height) {
        if (textureId == 0) return null;
        GL11.glBindTexture(3553, textureId);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glGetTexImage(3553, 0, 6408, 5121, buffer);
        return buffer;
    }

    private static void writeFramebuffer(File dir, Minecraft mc) throws Exception {
        var target = mc.getMainRenderTarget();
        int width = target.width;
        int height = target.height;
        target.bindRead();
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, 6408, 5121, buffer);
        target.unbindRead();
        write(dir, "frame", buffer, width, height);
    }

    private static void write(File dir, String name, ByteBuffer buffer, int width, int height) throws Exception {
        if (buffer == null) return;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = (y * width + x) * 4;
                int r = buffer.get(i) & 255;
                int g = buffer.get(i + 1) & 255;
                int b = buffer.get(i + 2) & 255;
                image.setRGB(x, height - 1 - y, (r << 16) | (g << 8) | b);
            }
        }
        ImageIO.write(image, "png", new File(dir, name + ".png"));
    }
}
