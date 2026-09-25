package com.truenins;

import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;

public final class TrueNinsTags {

    public enum Kind { TRUEME, ARMOR, WEAPON }

    public record Def(String key, String command, Kind kind, ChatFormatting color, String icon) {

        public String descKey() {
            return "truenins.tag." + this.key + ".desc";
        }

        public String nameKey() {
            return "truenins.tag." + this.key + ".name";
        }
    }

    public static final List<Def> ALL = List.of(
        new Def("true_me",    "trueme",    Kind.TRUEME, ChatFormatting.GOLD,         "\u2694"),
        new Def("deity",      "deity",     Kind.ARMOR,  ChatFormatting.LIGHT_PURPLE, "\u2727"),
        new Def("fly",        "fly",       Kind.ARMOR,  ChatFormatting.AQUA,         "\u27E1"),
        new Def("immunity",   "immunity",  Kind.ARMOR,  ChatFormatting.YELLOW,       "\u2624"),
        new Def("thorns",     "thorns",    Kind.ARMOR,  ChatFormatting.RED,          "\u25C8"),
        new Def("adaptation", "adaptation",Kind.ARMOR,  ChatFormatting.GREEN,        "\u27F3"),
        new Def("mitigation", "mitigation",Kind.ARMOR,  ChatFormatting.DARK_AQUA,    "\u25A6"),
        new Def("revive",     "revive",    Kind.ARMOR,  ChatFormatting.YELLOW,       "\u271A"),
        new Def("muryokusho",  "muryokusho",    Kind.WEAPON, ChatFormatting.DARK_RED,     "\u2297"),
        new Def("antiheal",   "antiheal",  Kind.WEAPON, ChatFormatting.DARK_RED,     "\u2715"),
        new Def("justice",    "justice",   Kind.WEAPON, ChatFormatting.GOLD,         "\u2696"),
        new Def("scaling",    "scaling",   Kind.WEAPON, ChatFormatting.BLUE,         "\u2197"),
        new Def("malice",     "malice",    Kind.WEAPON, ChatFormatting.DARK_PURPLE,  "\u2620"),
        new Def("resolve",    "resolve",   Kind.ARMOR,  ChatFormatting.DARK_RED,     "\uD83D\uDEE1"),
        new Def("siphon",     "siphon",    Kind.WEAPON, ChatFormatting.RED,          "\u2764"),
        new Def("rage",       "rage",      Kind.WEAPON, ChatFormatting.RED,          "\u2600"),
        new Def("unclear",    "unclear",   Kind.WEAPON, ChatFormatting.GRAY,         "\u2298"),
        new Def("colorfast",  "colorfast", Kind.WEAPON, ChatFormatting.DARK_GREEN,   "\u2726"),
        new Def("blackflash", "blackflash",Kind.WEAPON, ChatFormatting.DARK_RED,     "\u2722"),
        new Def("infinity",   "infinity",  Kind.ARMOR,  ChatFormatting.AQUA,         "\u221E")
    );

    public static final List<String> KEYS;

    public static final String[] REMOVABLE;

    public static final String COMMAND_LIST;

    static {
        List<String> keys = new ArrayList<>(ALL.size());
        List<String> commands = new ArrayList<>(ALL.size());
        for (Def def : ALL) {
            keys.add(def.key());
            commands.add(def.command());
        }
        KEYS = List.copyOf(keys);
        COMMAND_LIST = String.join("|", commands);

        List<String> removable = new ArrayList<>(keys);
        removable.add("TrueDamageAmount");
        REMOVABLE = removable.toArray(new String[0]);
    }

    public static Def byKey(String key) {
        for (Def def : ALL) {
            if (def.key().equals(key)) return def;
        }
        return null;
    }

    private TrueNinsTags() {}
}
