package com.truenins.client;

import com.truenins.TrueNinsEnchantments;
import com.truenins.TrueNinsTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TagGuideScreen extends Screen {

    private static final int PANEL_BG       = 0xF0242008;
    private static final int PANEL_EDGE     = 0xFFB8A23C;
    private static final int PANEL_EDGE_IN  = 0xFF4A4318;
    private static final int TITLE_COLOR    = 0xFFFFE95C;
    private static final int SECTION_COLOR  = 0xFFE8D75A;
    private static final int NAME_FALLBACK  = 0xFFFFFFFF;
    private static final int DESC_ZH        = 0xFFD8CE8A;
    private static final int DESC_EN        = 0xFF95A8BC;
    private static final int HINT_COLOR     = 0xFFA89A50;
    private static final int SCROLL_TRACK   = 0x30FFE95C;
    private static final int SCROLL_THUMB   = 0xFFE8D75A;
    private static final int ARROW_ON       = 0xFFFFE95C;
    private static final int ARROW_OFF      = 0xFF6A6330;
    private static final int ENCH_NAME      = 0xFFB9E6FF;

    private static final int PAD = 10;
    private static final int PAGES = 2;

    private static final String HEADER_TAGS = "\u8BCD\u6761  \u00B7  TAGS";
    private static final String HEADER_ENCH = "\u9644\u9B54  \u00B7  ENCHANTMENTS";

    private record Entry(Component name, List<FormattedCharSequence> lines, int height) {}

    private final List<Entry> entries = new ArrayList<>();

    private Map<String, String> langZh = new HashMap<>();
    private Map<String, String> langEn = new HashMap<>();

    private int panelX, panelY, panelW, panelH;
    private int listLeft, listRight, listTop, listBottom;
    private int contentHeight;
    private double scroll;
    private int page;

    private int prevX1, prevX2, prevY1, prevY2;
    private int nextX1, nextX2, nextY1, nextY2;

    public TagGuideScreen() {
        super(Component.translatable("truenins.guide.title"));
    }

    @Override
    protected void init() {
        this.langZh = loadLang("zh_cn");
        this.langEn = loadLang("en_us");

        this.panelW = Math.min(360, this.width - 40);
        this.panelH = Math.min(230, this.height - 40);
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;

        this.listLeft = this.panelX + PAD;
        this.listRight = this.panelX + this.panelW - PAD - 6;
        this.listTop = this.panelY + 36;
        this.listBottom = this.panelY + this.panelH - 20;

        this.prevX1 = this.panelX + PAD;
        this.prevY1 = this.panelY + 7;
        this.prevX2 = this.prevX1 + 12;
        this.prevY2 = this.prevY1 + 10;

        this.nextX2 = this.panelX + this.panelW - PAD;
        this.nextX1 = this.nextX2 - 12;
        this.nextY1 = this.panelY + 7;
        this.nextY2 = this.nextY1 + 10;

        this.rebuild();
    }

    private static Map<String, String> loadLang(String code) {
        Map<String, String> out = new HashMap<>();
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return out;
            ResourceLocation id = new ResourceLocation("truenins", "lang/" + code + ".json");
            Optional<Resource> res = mc.getResourceManager().getResource(id);
            if (res.isEmpty()) return out;
            try (BufferedReader reader = res.get().openAsReader()) {
                for (Map.Entry<String, com.google.gson.JsonElement> e : GsonHelper.parse(reader).entrySet()) {
                    if (e.getValue().isJsonPrimitive()) out.put(e.getKey(), e.getValue().getAsString());
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private String zh(String key, String fallback) {
        String v = this.langZh.get(key);
        return v != null ? v : fallback;
    }

    private String en(String key, String fallback) {
        String v = this.langEn.get(key);
        return v != null ? v : fallback;
    }

    private void rebuild() {
        this.entries.clear();
        int wrapWidth = this.listRight - this.listLeft - 12;
        int total = 0;

        if (this.page == 0) {
            for (TrueNinsTags.Def def : TrueNinsTags.ALL) {
                String fallbackName = def.command();
                String zhName = this.zh(def.nameKey(), fallbackName);
                String enName = this.en(def.nameKey(), fallbackName);

                Component name = Component.literal(def.icon() + " ").withStyle(def.color())
                    .copy()
                    .append(Component.literal(zhName).withStyle(def.color()))
                    .append(Component.literal("   " + enName).withStyle(style -> style.withColor(DESC_EN)));

                List<FormattedCharSequence> lines = new ArrayList<>();
                lines.addAll(this.font.split(Component.literal(this.zh(def.descKey(), "")).withStyle(style -> style.withColor(DESC_ZH)), wrapWidth));
                lines.addAll(this.font.split(Component.literal(this.en(def.descKey(), "")).withStyle(style -> style.withColor(DESC_EN)), wrapWidth));

                int height = this.font.lineHeight * (1 + lines.size()) + 7;
                this.entries.add(new Entry(name, lines, height));
                total += height;
            }
        } else {
            for (TrueNinsEnchantments.Def def : TrueNinsEnchantments.DEFS) {
                String nameKey = "enchantment.truenins." + def.id();
                String descKey = nameKey + ".desc";
                String plainName = this.zh(nameKey, def.id());

                Component name = Component.literal(plainName).withStyle(style -> style.withColor(ENCH_NAME));
                if (def.maxLevel() > 1) {
                    name = name.copy().append(Component.literal("   \u6700\u9AD8 " + def.maxLevel() + " \u7EA7").withStyle(style -> style.withColor(HINT_COLOR)));
                }

                List<FormattedCharSequence> lines = new ArrayList<>(
                    this.font.split(Component.literal(this.zh(descKey, "")).withStyle(style -> style.withColor(DESC_ZH)), wrapWidth));

                int height = this.font.lineHeight * (1 + lines.size()) + 7;
                this.entries.add(new Entry(name, lines, height));
                total += height;
            }
        }

        this.contentHeight = total;
        this.scroll = 0;
    }

    private void turn(int delta) {
        this.page = (this.page + delta + PAGES) % PAGES;
        this.rebuild();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, PANEL_BG);
        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + 1, PANEL_EDGE);
        graphics.fill(this.panelX, this.panelY + this.panelH - 1, this.panelX + this.panelW, this.panelY + this.panelH, PANEL_EDGE);
        graphics.fill(this.panelX, this.panelY, this.panelX + 1, this.panelY + this.panelH, PANEL_EDGE);
        graphics.fill(this.panelX + this.panelW - 1, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, PANEL_EDGE);
        graphics.fill(this.panelX + 3, this.panelY + 3, this.panelX + this.panelW - 3, this.panelY + 4, PANEL_EDGE_IN);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.panelY + 8, TITLE_COLOR);

        graphics.drawString(this.font, "\u25C0", this.prevX1, this.prevY1, hovered(mouseX, mouseY, prevX1, prevY1, prevX2, prevY2) ? ARROW_ON : ARROW_OFF);
        graphics.drawString(this.font, "\u25B6", this.nextX1, this.nextY1, hovered(mouseX, mouseY, nextX1, nextY1, nextX2, nextY2) ? ARROW_ON : ARROW_OFF);

        String header = this.page == 0 ? HEADER_TAGS : HEADER_ENCH;
        int headerWidth = this.font.width(header);
        int headerLeft = this.width / 2 - headerWidth / 2;
        graphics.drawCenteredString(this.font, Component.literal(header), this.width / 2, this.panelY + 21, SECTION_COLOR);
        graphics.fill(headerLeft - 16, this.panelY + 26, headerLeft - 4, this.panelY + 27, PANEL_EDGE);
        graphics.fill(headerLeft + headerWidth + 4, this.panelY + 26, headerLeft + headerWidth + 16, this.panelY + 27, PANEL_EDGE);

        graphics.enableScissor(this.listLeft, this.listTop, this.listRight, this.listBottom);
        int y = this.listTop - (int) this.scroll;
        for (Entry entry : this.entries) {
            graphics.drawString(this.font, entry.name(), this.listLeft, y, NAME_FALLBACK);
            y += this.font.lineHeight;
            for (FormattedCharSequence line : entry.lines()) {
                graphics.drawString(this.font, line, this.listLeft + 12, y, NAME_FALLBACK);
                y += this.font.lineHeight;
            }
            y += 7;
        }
        graphics.disableScissor();

        this.renderScrollbar(graphics);

        Component hint = Component.translatable("truenins.guide.page", this.page + 1, PAGES, this.entries.size());
        graphics.drawCenteredString(this.font, hint, this.width / 2, this.panelY + this.panelH - 14, HINT_COLOR);
    }

    private static boolean hovered(int mx, int my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int viewHeight = this.listBottom - this.listTop;
        int maxScroll = this.contentHeight - viewHeight;
        if (maxScroll <= 0) return;

        int barLeft = this.listRight + 2;
        int barRight = this.listRight + 5;
        int thumbHeight = Math.max(14, viewHeight * viewHeight / this.contentHeight);
        int thumbY = this.listTop + (int) ((viewHeight - thumbHeight) * (this.scroll / maxScroll));

        graphics.fill(barLeft, this.listTop, barRight, this.listBottom, SCROLL_TRACK);
        graphics.fill(barLeft, thumbY, barRight, thumbY + thumbHeight, SCROLL_THUMB);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered((int) mouseX, (int) mouseY, prevX1, prevY1, prevX2, prevY2)) {
            this.turn(-1);
            return true;
        }
        if (hovered((int) mouseX, (int) mouseY, nextX1, nextY1, nextX2, nextY2)) {
            this.turn(1);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            this.turn(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            this.turn(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int viewHeight = this.listBottom - this.listTop;
        int maxScroll = Math.max(0, this.contentHeight - viewHeight);
        this.scroll = Math.max(0, Math.min(maxScroll, this.scroll - delta * 14));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
