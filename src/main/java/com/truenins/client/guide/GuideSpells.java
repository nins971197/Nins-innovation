package com.truenins.client.guide;

import com.truenins.TrueNinsConfig;

import java.util.List;
import java.util.function.IntSupplier;

/**
 * The ten curses TrueNins hands to Iron's Spellbooks, in the order the guide lists them.
 *
 * <p>Mana and cooldown are suppliers rather than values so the page always shows what the config
 * currently says; the guide is a reference, so a stale hardcoded number would be worse than no
 * number. Nothing here touches the compat package: this file must load on installs without Iron's,
 * otherwise the whole guide screen would fail to initialise.
 */
public final class GuideSpells {

    /** One spell row. {@code manaPerSecond} is true for the only channelled spell. */
    public record Spell(String id, IntSupplier mana, boolean manaPerSecond, IntSupplier cooldownSeconds) {}

    public static final List<Spell> ALL = List.of(
        new Spell("infinity", TrueNinsConfig::infinityManaCost, false, TrueNinsConfig::infinityCooldownSeconds),
        new Spell("ao", TrueNinsConfig::aoManaCost, false, TrueNinsConfig::aoCooldownSeconds),
        new Spell("he", TrueNinsConfig::heManaCost, false, TrueNinsConfig::heCooldownSeconds),
        new Spell("purple", TrueNinsConfig::purpleManaCost, false, TrueNinsConfig::purpleCooldownSeconds),
        new Spell("unlimited_void", TrueNinsConfig::voidManaCost, false, TrueNinsConfig::voidCooldownSeconds),
        new Spell("shingan", TrueNinsConfig::shinganManaCost, false, TrueNinsConfig::shinganCooldownSeconds),
        new Spell("made_in_heaven", TrueNinsConfig::madeInHeavenManaPerSecond, true, TrueNinsConfig::madeInHeavenCooldownSeconds),
        new Spell("curse_zone", TrueNinsConfig::curseZoneManaCost, false, TrueNinsConfig::curseZoneCooldownSeconds),
        new Spell("deadly_sentencing", TrueNinsConfig::judgmentManaCost, false, TrueNinsConfig::judgmentCooldownSeconds),
        new Spell("eternal_conflagration", TrueNinsConfig::conflagrationManaCost, false, TrueNinsConfig::conflagrationCooldownSeconds)
    );

    private GuideSpells() {}
}
