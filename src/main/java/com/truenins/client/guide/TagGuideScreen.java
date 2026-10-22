package com.truenins.client.guide;

import com.truenins.gameplay.enchantment.TrueNinsEnchantments;
import com.truenins.gameplay.tag.TrueNinsTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;

public class TagGuideScreen extends Screen {

    // Gold-on-vellum palette: every piece of chrome shares the same three golds so the
    // book reads as one artefact, and entry names are the only bold text on the page.
    private static final int PANEL_BG       = 0xF21A1509;
    private static final int PANEL_EDGE     = 0xFFD9AE56;
    private static final int PANEL_EDGE_IN  = 0xFF4A3A1C;
    private static final int TITLE_COLOR    = 0xFFFFE6A8;
    private static final int SECTION_COLOR  = 0xFFFFD166;
    private static final int NAME_FALLBACK  = 0xFFFFD979;
    private static final int NAME_GOLD      = 0xFFFFD979;
    private static final int DESC_ZH        = 0xFFDCCBA2;
    private static final int DESC_EN        = 0xFF9E8A61;
    private static final int HINT_COLOR     = 0xFFC2A468;
    private static final int SCROLL_TRACK   = 0x504A3A1C;
    private static final int SCROLL_THUMB   = 0xFFD9AE56;
    private static final int ARROW_ON       = 0xFFFFE9B0;
    private static final int ARROW_OFF      = 0xFF6E5C33;
    private static final int ENCH_NAME      = 0xFFFFD979;

    private static final int PAD = 12;

    private static final String HEADER_TAGS = "\u8BCD\u6761  \u00B7  TAGS";
    private static final String HEADER_ENCH = "\u9644\u9B54  \u00B7  ENCHANTMENTS";
    private static final String HEADER_SPELLS = "\u8054\u52A8\u6CD5\u672F  \u00B7  IRON'S SPELLBOOKS";

    /** The Iron's page is only offered when the mod is actually there to cast with. */
    private static int pageCount() {
        return ModList.get().isLoaded("irons_spellbooks") ? 3 : 2;
    }

    private record Entry(String searchText, List<FormattedCharSequence> lines, int height) {}

    private final List<Entry> entries = new ArrayList<>();

    private Map<String, String> langZh = new HashMap<>();
    private Map<String, String> langEn = new HashMap<>();

    private int panelX, panelY, panelW, panelH;
    private int listLeft, listRight, listTop, listBottom;
    private int contentHeight;
    private double scroll;
    private int page;
    private EditBox search;
    private boolean draggingScrollbar;
    private double scrollbarGrabOffset;
    private List<Entry> filteredEntries = List.of();

    private int prevX1, prevX2, prevY1, prevY2;
    private int nextX1, nextX2, nextY1, nextY2;

    public TagGuideScreen() {
        super(Component.translatable("truenins.guide.title"));
    }

    @Override
    protected void init() {
        this.langZh = loadLang("zh_cn");
        this.langEn = loadLang("en_us");
        this.draggingScrollbar = false;

        this.panelW = Math.max(1, Math.min(520, this.width - 16));
        this.panelH = Math.max(1, Math.min(340, this.height - 16));
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;

        this.listLeft = this.panelX + PAD;
        this.listRight = this.panelX + this.panelW - PAD - 6;
        this.listTop = this.panelY + 60;
        this.listBottom = this.panelY + this.panelH - 26;

        this.search = new EditBox(this.font, this.panelX + PAD, this.panelY + 37,
            this.panelW - PAD * 2, 16, Component.translatable("truenins.guide.search"));
        this.search.setMaxLength(48);
        this.search.setHint(Component.translatable("truenins.guide.search"));
        this.search.setResponder(value -> {
            this.scroll = 0;
            updateFilter();
        });
        this.addRenderableWidget(this.search);

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
        if (this.page >= pageCount()) this.page = 0;
        int wrapWidth = Math.max(16, this.listRight - this.listLeft - 18);

        if (this.page == 2) {
            rebuildSpells(wrapWidth);
        } else if (this.page == 0) {
            for (TrueNinsTags.Def def : TrueNinsTags.ALL) {
                String fallbackName = def.command();
                String zhName = this.zh(def.nameKey(), fallbackName);
                String enName = this.en(def.nameKey(), fallbackName);

                Component name = Component.literal(def.icon() + " ").withStyle(style -> style.withBold(true).withColor(NAME_GOLD))
                    .copy()
                    .append(Component.literal(zhName).withStyle(style -> style.withBold(true).withColor(NAME_GOLD)))
                    .append(Component.literal("   " + enName).withStyle(style -> style.withColor(DESC_EN)));

                List<FormattedCharSequence> lines = new ArrayList<>();
                lines.addAll(this.font.split(Component.literal(this.zh(def.descKey(), "")).withStyle(style -> style.withColor(DESC_ZH)), wrapWidth));
                lines.addAll(this.font.split(Component.literal(this.en(def.descKey(), "")).withStyle(style -> style.withColor(DESC_EN)), wrapWidth));

                List<FormattedCharSequence> titleLines = this.font.split(name, wrapWidth);
                lines.addAll(0, titleLines);
                int height = this.font.lineHeight * lines.size() + 8;
                this.entries.add(new Entry((def.command() + " " + zhName + " " + enName + " "
                    + this.zh(def.descKey(), "") + " " + this.en(def.descKey(), ""))
                    .toLowerCase(Locale.ROOT), lines, height));
            }
        } else {
            for (TrueNinsEnchantments.Def def : TrueNinsEnchantments.DEFS) {
                String nameKey = "enchantment.truenins." + def.id();
                String descKey = nameKey + ".desc";
                String plainName = this.zh(nameKey, def.id());
                String englishName = this.en(nameKey, def.id());

                Component name = Component.literal(plainName)
                    .withStyle(style -> style.withBold(true).withColor(ENCH_NAME));
                if (def.maxLevel() > 1) {
                    name = name.copy().append(Component.translatable("truenins.guide.maxlevel", def.maxLevel())
                        .withStyle(style -> style.withColor(HINT_COLOR)));
                }

                List<FormattedCharSequence> lines = new ArrayList<>(
                    this.font.split(Component.literal(this.zh(descKey, "")).withStyle(style -> style.withColor(DESC_ZH)), wrapWidth));
                lines.addAll(this.font.split(Component.literal(this.en(descKey, ""))
                    .withStyle(style -> style.withColor(DESC_EN)), wrapWidth));

                List<FormattedCharSequence> titleLines = this.font.split(name, wrapWidth);
                lines.addAll(0, titleLines);
                int height = this.font.lineHeight * lines.size() + 8;
                this.entries.add(new Entry((plainName + " " + englishName + " " + def.id() + " "
                    + this.zh(descKey, "") + " " + this.en(descKey, ""))
                    .toLowerCase(Locale.ROOT), lines, height));
            }
        }

        this.scroll = 0;
        updateFilter();
    }

    /** Page three lives only behind the Iron's Spellbooks check; it never touches the compat package. */
    private void rebuildSpells(int wrapWidth) {
        List<FormattedCharSequence> note = new ArrayList<>();
        note.addAll(this.font.split(Component.literal(this.zh("truenins.guide.spell.note",
            "\u53EF\u9009\u8054\u52A8\uFF1A\u53EA\u6709\u88C5\u4E86\u94C1\u9B54\u6CD5\uFF08Iron's Spellbooks\uFF09\uFF0C\u8FD9\u4E00\u9875\u7684\u5492\u672F\u624D\u4F1A\u5B58\u5728\uFF1B\u6CA1\u88C5\u5C31\u770B\u4E0D\u5230\uFF0C\u4E5F\u653E\u4E0D\u51FA\u6765\u3002"))
            .withStyle(style -> style.withColor(HINT_COLOR)), wrapWidth));
        note.addAll(this.font.split(Component.literal(this.en("truenins.guide.spell.note",
            "Optional compatibility: these curses exist only when Iron's Spellbooks is installed."))
            .withStyle(style -> style.withColor(DESC_EN)), wrapWidth));
        this.entries.add(new Entry("optional irons spellbooks \u94C1\u9B54\u6CD5 \u8054\u52A8", note,
            this.font.lineHeight * note.size() + 8));

        for (GuideSpells.Spell spell : GuideSpells.ALL) {
            String nameKey = "spell.truenins." + spell.id();
            String zhName = this.zh(nameKey, spell.id());
            String enName = this.en(nameKey, spell.id());
            String zhDesc = this.zh("truenins.guide.spell." + spell.id() + ".desc", "");
            String enDesc = this.en("truenins.guide.spell." + spell.id() + ".desc", "");

            Component name = Component.literal(zhName).withStyle(style -> style.withBold(true).withColor(ENCH_NAME))
                .copy().append(Component.literal("   " + enName).withStyle(style -> style.withColor(DESC_EN)));

            int mana = spell.mana().getAsInt();
            int cooldown = spell.cooldownSeconds().getAsInt();
            String manaKey = spell.manaPerSecond() ? "truenins.guide.spell.mana_rate" : "truenins.guide.spell.mana";
            String manaZh = String.format(this.zh(manaKey, "\u6CD5\u529B %s"), mana);
            String manaEn = String.format(this.en(manaKey, "Mana %s"), mana);
            String coolZh = cooldown > 0
                ? String.format(this.zh("truenins.guide.spell.cooldown", "\u51B7\u5374 %s \u79D2"), cooldown)
                : this.zh("truenins.guide.spell.cooldown_none", "\u65E0\u51B7\u5374");
            String coolEn = cooldown > 0
                ? String.format(this.en("truenins.guide.spell.cooldown", "%s s cooldown"), cooldown)
                : this.en("truenins.guide.spell.cooldown_none", "no cooldown");

            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.addAll(this.font.split(name, wrapWidth));
            lines.addAll(this.font.split(Component.literal(manaZh + "  \u00B7  " + coolZh)
                .withStyle(style -> style.withColor(HINT_COLOR)), wrapWidth));
            lines.addAll(this.font.split(Component.literal(manaEn + "  \u00B7  " + coolEn)
                .withStyle(style -> style.withColor(DESC_EN)), wrapWidth));
            lines.addAll(this.font.split(Component.literal(zhDesc).withStyle(style -> style.withColor(DESC_ZH)), wrapWidth));
            lines.addAll(this.font.split(Component.literal(enDesc).withStyle(style -> style.withColor(DESC_EN)), wrapWidth));

            this.entries.add(new Entry((zhName + " " + enName + " " + spell.id() + " " + zhDesc + " " + enDesc)
                .toLowerCase(Locale.ROOT), lines, this.font.lineHeight * lines.size() + 8));
        }
    }

    private void updateFilter() {
        String needle = this.search == null ? "" : this.search.getValue().trim().toLowerCase(Locale.ROOT);
        this.filteredEntries = needle.isEmpty() ? List.copyOf(this.entries)
            : this.entries.stream().filter(entry -> entry.searchText().contains(needle)).toList();
        this.contentHeight = this.filteredEntries.stream().mapToInt(Entry::height).sum();
        this.scroll = Math.min(this.scroll, Math.max(0, this.contentHeight - (this.listBottom - this.listTop)));
    }

    private void turn(int delta) {
        int pages = pageCount();
        this.page = (this.page + delta + pages) % pages;
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
        graphics.fill(this.panelX + PAD, this.panelY + 34,
            this.panelX + this.panelW - PAD, this.panelY + 35, PANEL_EDGE_IN);

        graphics.drawCenteredString(this.font, this.title.copy().withStyle(style -> style.withBold(true)),
            this.width / 2, this.panelY + 8, TITLE_COLOR);

        graphics.drawString(this.font, "\u25C0", this.prevX1, this.prevY1, hovered(mouseX, mouseY, prevX1, prevY1, prevX2, prevY2) ? ARROW_ON : ARROW_OFF);
        graphics.drawString(this.font, "\u25B6", this.nextX1, this.nextY1, hovered(mouseX, mouseY, nextX1, nextY1, nextX2, nextY2) ? ARROW_ON : ARROW_OFF);

        String header = this.page == 0 ? HEADER_TAGS : this.page == 1 ? HEADER_ENCH : HEADER_SPELLS;
        int headerWidth = this.font.width(header);
        int headerLeft = this.width / 2 - headerWidth / 2;
        graphics.drawCenteredString(this.font, Component.literal(header), this.width / 2, this.panelY + 21, SECTION_COLOR);
        graphics.fill(headerLeft - 16, this.panelY + 26, headerLeft - 4, this.panelY + 27, PANEL_EDGE);
        graphics.fill(headerLeft + headerWidth + 4, this.panelY + 26, headerLeft + headerWidth + 16, this.panelY + 27, PANEL_EDGE);

        graphics.enableScissor(this.listLeft, this.listTop, this.listRight, this.listBottom);
        int y = this.listTop - (int) this.scroll;
        for (Entry entry : this.filteredEntries) {
            if (y >= this.listBottom) break;
            if (y + entry.height() <= this.listTop) {
                y += entry.height();
                continue;
            }
            for (FormattedCharSequence line : entry.lines()) {
                graphics.drawString(this.font, line, this.listLeft + 4, y, NAME_FALLBACK);
                y += this.font.lineHeight;
            }
            graphics.fill(this.listLeft + 4, y + 3, this.listRight - 4, y + 4, PANEL_EDGE_IN);
            y += 8;
        }
        graphics.disableScissor();

        if (this.filteredEntries.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("truenins.guide.empty"),
                this.width / 2, this.listTop + 12, HINT_COLOR);
        }

        this.renderScrollbar(graphics);

        Component hint = Component.translatable("truenins.guide.page", this.page + 1, pageCount(), this.filteredEntries.size());
        graphics.drawCenteredString(this.font, hint, this.width / 2, this.panelY + this.panelH - 14, HINT_COLOR);

        // Render child widgets after the panel so the search field remains interactive and visible.
        super.render(graphics, mouseX, mouseY, partialTick);
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
        if (button == 0 && this.contentHeight > this.listBottom - this.listTop
            && hovered((int) mouseX, (int) mouseY, this.listRight + 2, this.listTop, this.listRight + 6, this.listBottom)) {
            this.draggingScrollbar = true;
            int viewHeight = this.listBottom - this.listTop;
            int thumbHeight = Math.max(14, viewHeight * viewHeight / this.contentHeight);
            double thumbTop = this.listTop + (viewHeight - thumbHeight) * this.scroll
                / (this.contentHeight - viewHeight);
            this.scrollbarGrabOffset = mouseY >= thumbTop && mouseY <= thumbTop + thumbHeight
                ? mouseY - thumbTop : thumbHeight * 0.5D;
            scrollTo(mouseY);
            return true;
        }
        if (button == 0 && hovered((int) mouseX, (int) mouseY, prevX1, prevY1, prevX2, prevY2)) {
            this.turn(-1);
            return true;
        }
        if (button == 0 && hovered((int) mouseX, (int) mouseY, nextX1, nextY1, nextX2, nextY2)) {
            this.turn(1);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingScrollbar) {
            scrollTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void scrollTo(double mouseY) {
        int viewHeight = this.listBottom - this.listTop;
        int maxScroll = Math.max(0, this.contentHeight - viewHeight);
        int thumbHeight = Math.max(14, viewHeight * viewHeight / Math.max(1, this.contentHeight));
        double travel = Math.max(1, viewHeight - thumbHeight);
        this.scroll = maxScroll * Math.max(0.0D, Math.min(1.0D,
            (mouseY - this.listTop - this.scrollbarGrabOffset) / travel));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.search != null && this.search.isFocused()) return super.keyPressed(keyCode, scanCode, modifiers);
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
        if (!hovered((int) mouseX, (int) mouseY,
            this.panelX, this.listTop, this.panelX + this.panelW, this.listBottom)) return false;
        int viewHeight = this.listBottom - this.listTop;
        int maxScroll = Math.max(0, this.contentHeight - viewHeight);
        this.scroll = Math.max(0, Math.min(maxScroll, this.scroll - delta * 14));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.search != null) this.search.tick();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String query = this.search == null ? "" : this.search.getValue();
        boolean focused = this.search != null && this.search.isFocused();
        super.resize(minecraft, width, height);
        if (this.search != null) {
            this.search.setValue(query);
            this.search.setFocused(focused);
        }
        // setValue does not fire the responder, so the rebuilt list is unfiltered: reapply it.
        this.scroll = 0.0D;
        this.updateFilter();
    }
}
