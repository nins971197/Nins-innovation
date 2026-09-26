package com.truenins;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.Map;

public final class TrueNinsConfig {

    public static final ForgeConfigSpec COMMON_SPEC;

    public static final Map<String, ForgeConfigSpec.BooleanValue> ENCH_DISCOVERABLE = new HashMap<>();
    public static final Map<String, ForgeConfigSpec.BooleanValue> ENCH_TRADEABLE = new HashMap<>();
    public static final Map<String, ForgeConfigSpec.BooleanValue> ENCH_TREASURE_ONLY = new HashMap<>();

    public static final ForgeConfigSpec.BooleanValue ENCH_TOOLTIP_DESCRIPTIONS;

    public static final ForgeConfigSpec.DoubleValue THORNS_REFLECT_PERCENT;

    public static final ForgeConfigSpec.IntValue ADAPTATION_HIT_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue ADAPTATION_REDUCTION_PERCENT;
    public static final ForgeConfigSpec.IntValue ADAPTATION_RESET_TICKS;

    public static final ForgeConfigSpec.DoubleValue MITIGATION_BASE_CAP;
    public static final ForgeConfigSpec.DoubleValue MITIGATION_SCALE;

    public static final ForgeConfigSpec.IntValue REVIVE_COOLDOWN_SECONDS;

    public static final ForgeConfigSpec.DoubleValue MURYOKUSHO_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue MURYOKUSHO_COOLDOWN_SECONDS;

    public static ForgeConfigSpec.DoubleValue[] ANTIHEAL_LEVELS;

    public static final ForgeConfigSpec.IntValue ANTIHEAL_AMPLIFIER;
    public static final ForgeConfigSpec.IntValue ANTIHEAL_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue ANTIHEAL_COOLDOWN_SECONDS;

    public static final ForgeConfigSpec.DoubleValue JUSTICE_PERCENT;
    public static final ForgeConfigSpec.IntValue JUSTICE_COOLDOWN_SECONDS;

    public static final ForgeConfigSpec.DoubleValue SCALING_PERCENT;

    public static final ForgeConfigSpec.IntValue MALICE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue MALICE_PERCENT;
    public static final ForgeConfigSpec.IntValue MALICE_MAX_STACKS;
    public static final ForgeConfigSpec.IntValue MALICE_RESET_SECONDS;

    public static final ForgeConfigSpec.IntValue RESOLVE_DURATION_SECONDS;
    public static final ForgeConfigSpec.DoubleValue RESOLVE_DAMAGE_PERCENT;
    public static final ForgeConfigSpec.IntValue RESOLVE_COOLDOWN_SECONDS;

    public static final ForgeConfigSpec.DoubleValue SIPHON_PERCENT;

    public static final ForgeConfigSpec.DoubleValue RAGE_50_PERCENT;
    public static final ForgeConfigSpec.DoubleValue RAGE_70_PERCENT;
    public static final ForgeConfigSpec.DoubleValue RAGE_90_PERCENT;

    public static final ForgeConfigSpec.IntValue CURSE_ZONE_MANA_COST;
    public static final ForgeConfigSpec.IntValue CURSE_ZONE_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue CURSE_ZONE_BLACK_FLASH_CHANCE;
    public static final ForgeConfigSpec.IntValue CURSE_ZONE_MAX_TRIGGERS;
    public static final ForgeConfigSpec.IntValue CURSE_ZONE_MANA_REFUND;

    public static final ForgeConfigSpec.IntValue INFINITY_MANA_COST;
    public static final ForgeConfigSpec.IntValue INFINITY_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue INFINITY_DURATION_SECONDS;
    public static final ForgeConfigSpec.DoubleValue INFINITY_DRAIN_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue INFINITY_DRAIN_INTERVAL_SECONDS;

    public static final ForgeConfigSpec.IntValue MADE_IN_HEAVEN_MANA_PER_SECOND;
    public static final ForgeConfigSpec.IntValue MADE_IN_HEAVEN_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue MADE_IN_HEAVEN_DURATION_SECONDS;
    public static final ForgeConfigSpec.IntValue MADE_IN_HEAVEN_TIME_TICKS_PER_SECOND;
    public static final ForgeConfigSpec.IntValue MADE_IN_HEAVEN_RADIUS;
    public static final ForgeConfigSpec.BooleanValue MADE_IN_HEAVEN_ACCELERATE_WORLD;

    public static final ForgeConfigSpec.DoubleValue BLACKFLASH_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BLACKFLASH_LAUNCH_SPEED;
    public static final ForgeConfigSpec.DoubleValue BLACKFLASH_LAUNCH_LIFT;
    public static final ForgeConfigSpec.IntValue BLACKFLASH_LAUNCH_TICKS;
    public static final ForgeConfigSpec.BooleanValue BLACKFLASH_BURST_EFFECTS;

    public static final ForgeConfigSpec.IntValue AO_MANA_COST;
    public static final ForgeConfigSpec.IntValue AO_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue AO_DURATION_SECONDS;
    public static final ForgeConfigSpec.DoubleValue AO_CHARGE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue AO_FLIGHT_SPEED;
    public static final ForgeConfigSpec.DoubleValue AO_MAX_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue AO_PULL_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue AO_PULL_RADIUS;
    public static final ForgeConfigSpec.DoubleValue AO_DAMAGE_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue AO_DAMAGE_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.BooleanValue AO_CONSUME_BLOCKS;
    public static final ForgeConfigSpec.IntValue AO_CONSUME_RADIUS;
    public static final ForgeConfigSpec.IntValue AO_CONSUME_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue AO_CONSUME_PER_INTERVAL;
    public static final ForgeConfigSpec.IntValue AO_MAX_SHARDS;

    public static final ForgeConfigSpec.IntValue HE_MANA_COST;
    public static final ForgeConfigSpec.IntValue HE_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue HE_SPEED;
    public static final ForgeConfigSpec.DoubleValue HE_MAX_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue HE_MAX_LIFE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue HE_DAMAGE_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue HE_HIT_RADIUS;
    public static final ForgeConfigSpec.IntValue HE_BREAK_RADIUS;
    public static final ForgeConfigSpec.BooleanValue HE_BREAK_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue HE_TEAR_LENGTH;
    public static final ForgeConfigSpec.DoubleValue HE_TEAR_RADIUS;
    public static final ForgeConfigSpec.DoubleValue HE_BLAST_RADIUS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("thorns");
        b.comment(" Thorn (反弹) — reflects damage back to the attacker.");
        THORNS_REFLECT_PERCENT = b
            .comment(" Fraction of incoming damage reflected (0.0 = off, 1.0 = 100%)",
                "Default: 1.0 (100%)")
            .defineInRange("reflectPercent", 1.0D, 0.0D, 1.0D);
        b.pop();

        b.push("adaptation");
        b.comment(" Adaptation (适应) — reduces damage from repeated same-type hits.");
        ADAPTATION_HIT_THRESHOLD = b
            .comment(" Number of consecutive same-source hits before reduction activates",
                "Default: 3")
            .defineInRange("hitThreshold", 3, 1, 100);
        ADAPTATION_REDUCTION_PERCENT = b
            .comment(" Fraction of damage remaining after adaptation (0.0-1.0)",
                "0.5 = 50% reduction. Default: 0.5")
            .defineInRange("reductionPercent", 0.5D, 0.0D, 1.0D);
        ADAPTATION_RESET_TICKS = b
            .comment(" Ticks without incoming damage before adaptation resets",
                "20 ticks = 1 second. Default: 200 (10 seconds)")
            .defineInRange("resetTicks", 200, 20, 12000);
        b.pop();

        b.push("mitigation");
        b.comment(" Mitigation (动态减伤) — per-tick damage cap scaled to current health.",
            "Cap formula: BASE_CAP - (1 - health%) * BASE_CAP * SCALE");
        MITIGATION_BASE_CAP = b
            .comment(" Maximum damage allowed per tick at full health",
                "Default: 6.0 (low enough to be noticeable on normal hits)")
            .defineInRange("baseCap", 6.0D, 1.0D, 1000.0D);
        MITIGATION_SCALE = b
            .comment(" How much the cap shrinks as health drops (0.0-1.0)",
                "0.6 = at death's door the cap is 40% of baseCap. Default: 0.6")
            .defineInRange("scale", 0.6D, 0.0D, 1.0D);
        b.pop();

        b.push("revive");
        b.comment(" Revive (复活) — prevents death once, then goes on cooldown.",
            "Equip or hold any item with {revive: 1b}.");
        REVIVE_COOLDOWN_SECONDS = b
            .comment(" Cooldown between revives in seconds",
                "Default: 30")
            .defineInRange("cooldownSeconds", 30, 1, 3600);
        b.pop();

        b.push("muryokusho");
        b.comment(" muryokusho (无量空处) — weapon tag {muryokusho: 1b}.",
            "Hitting an entity freezes them: players cannot move/act,",
            "non-player entities lose AI. Cooldown is per TARGET.");
        MURYOKUSHO_DURATION_SECONDS = b
            .comment(" How long the target is frozen / AI-disabled (seconds)",
                "Default: 1.0")
            .defineInRange("durationSeconds", 1.0D, 0.1D, 60.0D);
        MURYOKUSHO_COOLDOWN_SECONDS = b
            .comment(" Cooldown per TARGET between muryokusho applications (seconds)",
                "Default: 10")
            .defineInRange("cooldownSeconds", 10, 0, 3600);
        b.pop();

        b.push("buff_antiheal");
        b.comment(" Healing reduction per amplifier level (0.0-1.0).",
            "Level 0 = amplifier 0, etc. 1.0 = 100% blocked.");
        double[] defaults = {0.3, 0.6, 0.9, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
        ANTIHEAL_LEVELS = new ForgeConfigSpec.DoubleValue[10];
        for (int i = 0; i < 10; i++) {
            ANTIHEAL_LEVELS[i] = b.defineInRange("level" + i, defaults[i], 0.0D, 1.0D);
        }
        b.pop();

        b.push("tag_antiheal");
        b.comment(" Antiheal tag — weapon tag {antiheal: 1b}.",
            "Hitting an entity applies the truenins:ban_healing effect.");
        ANTIHEAL_AMPLIFIER = b
            .comment(" Amplifier level of ban_healing applied (0-based, default: 3 = 100%)")
            .defineInRange("amplifier", 3, 0, 9);
        ANTIHEAL_DURATION_SECONDS = b
            .comment(" Duration of ban_healing applied (seconds, default: 10)")
            .defineInRange("durationSeconds", 10, 1, 600);
        ANTIHEAL_COOLDOWN_SECONDS = b
            .comment(" Cooldown per wielder (seconds, default: 0 = no cooldown)")
            .defineInRange("cooldownSeconds", 0, 0, 3600);
        b.pop();

        b.push("justice");
        b.comment(" Justice (审判) — reduces max HP by percentage on first hit.");
        JUSTICE_PERCENT = b.defineInRange("maxHpReductionPercent", 0.2D, 0.0D, 1.0D);
        JUSTICE_COOLDOWN_SECONDS = b.defineInRange("restoreCooldownSeconds", 10, 1, 3600);
        b.pop();

        b.push("scaling");
        b.comment(" Scaling (追加) — bonus damage based on target max HP.");
        SCALING_PERCENT = b.defineInRange("maxHpDamagePercent", 0.02D, 0.0D, 1.0D);
        b.pop();

        b.push("malice");
        b.comment(" Malice (咒怨) — stacking damage on same target.");
        MALICE_THRESHOLD = b.defineInRange("hitThreshold", 3, 1, 100);
        MALICE_PERCENT = b.defineInRange("stackPercent", 0.03D, 0.0D, 1.0D);
        MALICE_MAX_STACKS = b.defineInRange("maxStacks", 10, 1, 100);
        MALICE_RESET_SECONDS = b.defineInRange("resetSeconds", 10, 1, 3600);
        b.pop();

        b.push("resolve");
        b.comment(" Resolve (决心) — death prevention with countdown timer.");
        RESOLVE_DURATION_SECONDS = b.defineInRange("countdownSeconds", 10, 1, 600);
        RESOLVE_DAMAGE_PERCENT = b.defineInRange("finalDamagePercent", 0.7D, 0.0D, 1.0D);
        RESOLVE_COOLDOWN_SECONDS = b.defineInRange("cooldownSeconds", 180, 1, 3600);
        b.pop();

        b.push("siphon");
        b.comment(" Siphon (吸血) — heal based on damage dealt.");
        SIPHON_PERCENT = b.defineInRange("healPercent", 0.08D, 0.0D, 1.0D);
        b.pop();

        b.push("rage");
        b.comment(" Rage (狂暴) — damage bonus based on missing HP.");
        RAGE_50_PERCENT = b.defineInRange("bonusAt50Lost", 10.0D, 0.0D, 1000.0D);
        RAGE_70_PERCENT = b.defineInRange("bonusAt70Lost", 20.0D, 0.0D, 1000.0D);
        RAGE_90_PERCENT = b.defineInRange("bonusAt90Lost", 40.0D, 0.0D, 1000.0D);
        b.pop();

        b.push("spells");
        b.comment(" Iron's Spells 'n Spellbooks — the three truenins:curse spells.",
            "Every number below is authoritative: these spells read this file, not Iron's own",
            "spell config, so editing here takes effect without touching irons_spellbooks.",
            "Mana follows stock Iron's Spells: a spell book cast (or a sword, when",
            "swordsConsumeMana is on) pays the cost, while scrolls and every other cast",
            "source are free, and creative mode is free unless Iron's 'creativeMana' is on.");

        b.push("curse_zone");
        b.comment(" curse_zone (咒术zone) — opens a cursed zone around the caster.",
            "The zone has no time limit: it stays until it has produced its charges of",
            "Black Flashes, or until the caster casts it again (which refills the charges).");
        CURSE_ZONE_MANA_COST = b
            .comment(" Mana spent when the spell is cast",
                "Default: 250")
            .defineInRange("manaCost", 250, 0, 100000);
        CURSE_ZONE_COOLDOWN_SECONDS = b
            .comment(" Cooldown after the spell is cast (seconds)",
                "Default: 180 (3 minutes)")
            .defineInRange("cooldownSeconds", 180, 0, 86400);
        CURSE_ZONE_BLACK_FLASH_CHANCE = b
            .comment(" Chance per hit to trigger a Black Flash while the zone is open (percent)",
                "Default: 10")
            .defineInRange("blackFlashChancePercent", 10, 0, 100);
        CURSE_ZONE_MAX_TRIGGERS = b
            .comment(" How many Black Flashes the zone can produce before it closes",
                "Default: 3")
            .defineInRange("maxTriggers", 3, 1, 1000);
        CURSE_ZONE_MANA_REFUND = b
            .comment(" Mana given back for each Black Flash the zone itself triggered",
                "Default: 100")
            .defineInRange("manaRefundPerBlackFlash", 100, 0, 100000);
        b.pop();

        b.push("infinity");
        b.comment(" infinity (无下限) — the impassable wall.");
        INFINITY_MANA_COST = b
            .comment(" Mana spent when the spell is cast",
                "Default: 999")
            .defineInRange("manaCost", 999, 0, 100000);
        INFINITY_COOLDOWN_SECONDS = b
            .comment(" Cooldown after the spell is cast (seconds)",
                "Default: 60")
            .defineInRange("cooldownSeconds", 60, 0, 86400);
        INFINITY_DURATION_SECONDS = b
            .comment(" How long Infinity and Skyward stay active before the drain begins (seconds)",
                "Default: 30.0")
            .defineInRange("durationSeconds", 30.0D, 1.0D, 3600.0D);
        INFINITY_DRAIN_AMOUNT = b
            .comment(" Health drained per drain interval once the flight phase is over",
                "Default: 4.0 (2 hearts)")
            .defineInRange("drainAmount", 4.0D, 0.0D, 1000.0D);
        INFINITY_DRAIN_INTERVAL_SECONDS = b
            .comment(" Seconds between two health drains",
                "Default: 1.0")
            .defineInRange("drainIntervalSeconds", 1.0D, 0.05D, 600.0D);
        b.pop();

        b.push("made_in_heaven");
        b.comment(" made_in_heaven (天堂制造) — accelerates time and everything nearby.");
        MADE_IN_HEAVEN_DURATION_SECONDS = b
            .comment(" How long the channel lasts (seconds)",
                "Changing this needs a world rejoin, the channel length is read once at startup.",
                "Default: 30.0")
            .defineInRange("durationSeconds", 30.0D, 1.0D, 600.0D);
        MADE_IN_HEAVEN_MANA_PER_SECOND = b
            .comment(" Mana drained per second while channeling (200 = 100 charged every 10 ticks)",
                "Iron's charges a continuous spell once per 10 ticks and wants a full second",
                "of mana in reserve to keep the channel alive, so it ends by itself when the",
                "mana runs out. Default: 200")
            .defineInRange("manaPerSecond", 200, 0, 100000);
        MADE_IN_HEAVEN_COOLDOWN_SECONDS = b
            .comment(" Cooldown after the channel ends (seconds)",
                "Default: 120")
            .defineInRange("cooldownSeconds", 120, 0, 86400);
        MADE_IN_HEAVEN_TIME_TICKS_PER_SECOND = b
            .comment(" World time ticks added per second (20 ticks = 1 second of world time)",
                "Default: 3000 (= 150 per game tick, about 3.75 day/night cycles per cast)")
            .defineInRange("timeTicksPerSecond", 3000, 0, 480000);
        MADE_IN_HEAVEN_RADIUS = b
            .comment(" Radius in blocks around the caster that is also accelerated",
                "Blocks, block entities and mobs inside it tick once per game tick.",
                "Default: 8")
            .defineInRange("radius", 8, 0, 32);
        MADE_IN_HEAVEN_ACCELERATE_WORLD = b
            .comment(" Whether nearby blocks, block entities and mobs are accelerated too.",
                "Set to false to only accelerate the world clock.",
                "Default: true")
            .define("accelerateWorld", true);
        b.pop();

        b.push("ao");
        b.comment(" ao (苍) — a light blue sphere thrown from the hand that then orbits the caster,",
            "dragging nearby living entities onto itself and grinding them down.");
        AO_MANA_COST = b
            .comment(" Mana spent when the spell is cast",
                "Default: 200")
            .defineInRange("manaCost", 200, 0, 100000);
        AO_COOLDOWN_SECONDS = b
            .comment(" Cooldown after the spell is cast (seconds)",
                "Default: 30")
            .defineInRange("cooldownSeconds", 30, 0, 86400);
        AO_DURATION_SECONDS = b
            .comment(" How long the sphere exists before it bursts (seconds)",
                "Default: 15.0")
            .defineInRange("durationSeconds", 15.0D, 1.0D, 300.0D);
        AO_CHARGE_RADIUS = b
            .comment(" Radius of the circle the sphere flies while the spell is charging (blocks)",
                "It completes exactly one lap over the cast time. Default: 2.2")
            .defineInRange("chargeRadius", 2.2D, 0.5D, 12.0D);
        AO_FLIGHT_SPEED = b
            .comment(" How fast the sphere flies away once it is released (blocks per tick)",
                "Default: 0.45")
            .defineInRange("flightSpeed", 0.45D, 0.05D, 6.0D);
        AO_MAX_DISTANCE = b
            .comment(" How far it flies before it stops moving and keeps feeding in place (blocks)",
                "Default: 32.0")
            .defineInRange("maxDistance", 32.0D, 4.0D, 128.0D);
        AO_PULL_RADIUS = b
            .comment(" Extra radius beyond the orbit that entities are dragged in from (blocks)",
                "Default: 6.0")
            .defineInRange("pullRadius", 6.0D, 0.0D, 32.0D);
        AO_PULL_STRENGTH = b
            .comment(" How hard entities are dragged toward the sphere (blocks per tick)",
                "Default: 0.34")
            .defineInRange("pullStrength", 0.34D, 0.0D, 4.0D);
        AO_DAMAGE_AMOUNT = b
            .comment(" Damage dealt to everything touching the sphere",
                "Default: 1.5")
            .defineInRange("damageAmount", 1.5D, 0.0D, 1000.0D);
        AO_DAMAGE_INTERVAL_SECONDS = b
            .comment(" Seconds between two damage ticks",
                "Default: 0.5")
            .defineInRange("damageIntervalSeconds", 0.5D, 0.05D, 60.0D);
        AO_CONSUME_BLOCKS = b
            .comment(" Whether the sphere eats every block it passes through: each one is torn",
                "out, turned into a falling block shard and dragged into the sphere.",
                "Default: true")
            .define("consumeBlocks", true);
        AO_CONSUME_RADIUS = b
            .comment(" Radius around the sphere that it feeds on (blocks)",
                "Default: 6")
            .defineInRange("consumeRadius", 6, 1, 12);
        AO_CONSUME_INTERVAL_TICKS = b
            .comment(" Ticks between two sweeps",
                "1 means it chews every tick. Default: 1")
            .defineInRange("consumeIntervalTicks", 1, 1, 200);
        AO_CONSUME_PER_INTERVAL = b
            .comment(" Safety cap on how many blocks are removed per sweep, to stop huge radii",
                "from stalling the server. Default: 192")
            .defineInRange("consumePerInterval", 192, 1, 2048);
        AO_MAX_SHARDS = b
            .comment(" How many block shards can be carried at once. They are held around the",
                "sphere while it lasts and dropped where it ends, piling up there.",
                "Default: 600")
            .defineInRange("maxShards", 600, 8, 4096);
        b.pop();

        b.push("he");
        b.comment(" he (赫) — a deep red sphere thrown from the hand that flies dead straight,",
            "tearing through blocks and hitting everything on its path for heavy magic damage.");
        HE_MANA_COST = b
            .comment(" Mana spent when the spell is cast",
                "Default: 200")
            .defineInRange("manaCost", 200, 0, 100000);
        HE_COOLDOWN_SECONDS = b
            .comment(" Cooldown after the spell is cast (seconds)",
                "Default: 45")
            .defineInRange("cooldownSeconds", 45, 0, 86400);
        HE_SPEED = b
            .comment(" Flight speed (blocks per tick)",
                "Default: 0.95")
            .defineInRange("speed", 0.95D, 0.05D, 10.0D);
        HE_MAX_DISTANCE = b
            .comment(" Distance the sphere travels before it bursts (blocks)",
                "Default: 40.0")
            .defineInRange("maxDistance", 40.0D, 1.0D, 256.0D);
        HE_MAX_LIFE_SECONDS = b
            .comment(" Hard time limit for the flight (seconds)",
                "Default: 6.0")
            .defineInRange("maxLifeSeconds", 6.0D, 0.2D, 120.0D);
        HE_DAMAGE_AMOUNT = b
            .comment(" Magic damage dealt to each entity it touches (once per entity)",
                "Default: 30.0")
            .defineInRange("damageAmount", 30.0D, 0.0D, 10000.0D);
        HE_HIT_RADIUS = b
            .comment(" Hit radius around the sphere for entities (blocks)",
                "Default: 2.5")
            .defineInRange("hitRadius", 2.5D, 0.5D, 16.0D);
        HE_BREAK_BLOCKS = b
            .comment(" Whether the sphere destroys blocks on its path",
                "Default: true")
            .define("breakBlocks", true);
        HE_BREAK_RADIUS = b
            .comment(" Radius of the tunnel it carves while flying (blocks, 1 = a 3x3 tunnel)",
                "Default: 1")
            .defineInRange("breakRadius", 1, 1, 6);
        HE_TEAR_LENGTH = b
            .comment(" Half length along the flight axis of the rip left by the final blast",
                "Default: 8.0")
            .defineInRange("tearLength", 8.0D, 1.0D, 32.0D);
        HE_TEAR_RADIUS = b
            .comment(" Radius of that rip, perpendicular to the flight axis",
                "Default: 4.5")
            .defineInRange("tearRadius", 4.5D, 0.5D, 24.0D);
        HE_BLAST_RADIUS = b
            .comment(" How far the blast effect visually expands (blocks)",
                "Default: 6.0")
            .defineInRange("blastRadius", 6.0D, 1.0D, 32.0D);
        b.pop();

        b.pop();

        b.push("tag_blackflash");
        b.comment(" Black Flash (黑闪) — weapon tag {blackflash: <percent>} and the curse_zone spell.",
            "Triggering it multiplies that single hit and launches the target.");
        BLACKFLASH_DAMAGE_MULTIPLIER = b
            .comment(" Damage multiplier applied to the Black Flash hit",
                "Default: 10.0")
            .defineInRange("damageMultiplier", 10.0D, 1.0D, 1000.0D);
        BLACKFLASH_LAUNCH_SPEED = b
            .comment(" Horizontal launch speed given to the target (blocks per tick)",
                "Default: 2.10")
            .defineInRange("launchSpeed", 2.10D, 0.0D, 20.0D);
        BLACKFLASH_LAUNCH_LIFT = b
            .comment(" Upward speed given to the target (blocks per tick)",
                "Default: 0.085")
            .defineInRange("launchLift", 0.085D, 0.0D, 5.0D);
        BLACKFLASH_LAUNCH_TICKS = b
            .comment(" How many ticks the launch is kept up",
                "Default: 5")
            .defineInRange("launchTicks", 5, 1, 200);
        BLACKFLASH_BURST_EFFECTS = b
            .comment(" Whether to render the extra halo and shard particles on a Black Flash",
                "Turn off if the effect is too heavy for your pack.",
                "Default: true")
            .define("burstEffects", true);
        b.pop();

        b.push("enchantments");
        b.comment(" TrueNins enchantments — the enchantment counterpart of the NBT tags.",
            "Each entry has the three vanilla enchantment switches:",
            "  discoverable  may appear in the enchanting table",
            "  tradeable     may appear in villager trades",
            "  treasureOnly  treated as a treasure enchantment",
            "The defaults make every TrueNins enchantment treasure-only: they never",
            "show up in the enchanting table and are never traded.");
        for (TrueNinsEnchantments.Def def : TrueNinsEnchantments.DEFS) {
            b.push(def.id());
            b.comment(" " + def.id() + " — mirrors the '" + def.tagKey() + "' tag, max level " + def.maxLevel());
            ENCH_DISCOVERABLE.put(def.id(), b.define("discoverable", false));
            ENCH_TRADEABLE.put(def.id(), b.define("tradeable", false));
            ENCH_TREASURE_ONLY.put(def.id(), b.define("treasureOnly", true));
            b.pop();
        }
        ENCH_TOOLTIP_DESCRIPTIONS = b
            .comment(" Append TrueNins enchantment descriptions to item tooltips.",
                "Set to false if you run a mod that renders enchantment descriptions",
                "(for example Enchantment Descriptions), otherwise the text shows twice.")
            .define("showDescriptionsInTooltip", true);
        b.pop();

        COMMON_SPEC = b.build();
    }

    public static boolean enchantDiscoverable(String id) {
        return flag(ENCH_DISCOVERABLE, id, false);
    }

    public static boolean enchantTradeable(String id) {
        return flag(ENCH_TRADEABLE, id, false);
    }

    public static boolean enchantTreasureOnly(String id) {
        return flag(ENCH_TREASURE_ONLY, id, true);
    }

    public static boolean enchantTooltipDescriptions() {
        try {
            return ENCH_TOOLTIP_DESCRIPTIONS.get();
        } catch (Exception e) {
            return true;
        }
    }

    private static int integer(ForgeConfigSpec.IntValue value, int fallback) {
        if (value == null) return fallback;
        try {
            return value.get();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double real(ForgeConfigSpec.DoubleValue value, double fallback) {
        if (value == null) return fallback;
        try {
            return value.get();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean toggle(ForgeConfigSpec.BooleanValue value, boolean fallback) {
        if (value == null) return fallback;
        try {
            return value.get();
        } catch (Exception e) {
            return fallback;
        }
    }

    public static int curseZoneManaCost() {
        return integer(CURSE_ZONE_MANA_COST, 250);
    }

    public static int curseZoneCooldownSeconds() {
        return integer(CURSE_ZONE_COOLDOWN_SECONDS, 180);
    }

    public static int curseZoneBlackFlashChance() {
        return integer(CURSE_ZONE_BLACK_FLASH_CHANCE, 10);
    }

    public static int curseZoneMaxTriggers() {
        return integer(CURSE_ZONE_MAX_TRIGGERS, 3);
    }

    public static int curseZoneManaRefund() {
        return integer(CURSE_ZONE_MANA_REFUND, 100);
    }

    public static int infinityManaCost() {
        return integer(INFINITY_MANA_COST, 999);
    }

    public static int infinityCooldownSeconds() {
        return integer(INFINITY_COOLDOWN_SECONDS, 60);
    }

    public static int infinityDurationTicks() {
        return secondsToTicks(INFINITY_DURATION_SECONDS, 30.0D, 20);
    }

    public static float infinityDrainAmount() {
        return (float) real(INFINITY_DRAIN_AMOUNT, 4.0D);
    }

    public static int infinityDrainIntervalTicks() {
        return secondsToTicks(INFINITY_DRAIN_INTERVAL_SECONDS, 1.0D, 1);
    }

    public static int madeInHeavenManaPerSecond() {
        return integer(MADE_IN_HEAVEN_MANA_PER_SECOND, 200);
    }

    public static int madeInHeavenCooldownSeconds() {
        return integer(MADE_IN_HEAVEN_COOLDOWN_SECONDS, 120);
    }

    public static int madeInHeavenDurationTicks() {
        return secondsToTicks(MADE_IN_HEAVEN_DURATION_SECONDS, 30.0D, 20);
    }

    public static int madeInHeavenTimeTicksPerSecond() {
        return integer(MADE_IN_HEAVEN_TIME_TICKS_PER_SECOND, 3000);
    }

    public static int madeInHeavenRadius() {
        return integer(MADE_IN_HEAVEN_RADIUS, 8);
    }

    public static boolean madeInHeavenAccelerateWorld() {
        return toggle(MADE_IN_HEAVEN_ACCELERATE_WORLD, true);
    }

    public static float blackFlashDamageMultiplier() {
        return (float) real(BLACKFLASH_DAMAGE_MULTIPLIER, 10.0D);
    }

    public static double blackFlashLaunchSpeed() {
        return real(BLACKFLASH_LAUNCH_SPEED, 2.10D);
    }

    public static double blackFlashLaunchLift() {
        return real(BLACKFLASH_LAUNCH_LIFT, 0.085D);
    }

    public static int blackFlashLaunchTicks() {
        return integer(BLACKFLASH_LAUNCH_TICKS, 5);
    }

    public static boolean blackFlashBurstEffects() {
        return toggle(BLACKFLASH_BURST_EFFECTS, true);
    }

    public static int aoManaCost() {
        return integer(AO_MANA_COST, 200);
    }

    public static int aoCooldownSeconds() {
        return integer(AO_COOLDOWN_SECONDS, 30);
    }

    public static int aoDurationTicks() {
        return secondsToTicks(AO_DURATION_SECONDS, 15.0D, 20);
    }

    public static double aoChargeRadius() {
        return real(AO_CHARGE_RADIUS, 2.2D);
    }

    public static double aoFlightSpeed() {
        return real(AO_FLIGHT_SPEED, 0.45D);
    }

    public static double aoMaxDistance() {
        return real(AO_MAX_DISTANCE, 32.0D);
    }

    public static double aoPullStrength() {
        return real(AO_PULL_STRENGTH, 0.34D);
    }

    public static double aoPullRadius() {
        return real(AO_PULL_RADIUS, 6.0D);
    }

    public static float aoDamageAmount() {
        return (float) real(AO_DAMAGE_AMOUNT, 1.5D);
    }

    public static int aoDamageIntervalTicks() {
        return secondsToTicks(AO_DAMAGE_INTERVAL_SECONDS, 0.5D, 1);
    }

    public static boolean aoConsumeBlocks() {
        return toggle(AO_CONSUME_BLOCKS, true);
    }

    public static int aoConsumeRadius() {
        return integer(AO_CONSUME_RADIUS, 6);
    }

    public static int aoConsumeIntervalTicks() {
        return integer(AO_CONSUME_INTERVAL_TICKS, 1);
    }

    public static int aoConsumePerInterval() {
        return integer(AO_CONSUME_PER_INTERVAL, 48);
    }

    public static int aoMaxShards() {
        return integer(AO_MAX_SHARDS, 600);
    }

    public static int heManaCost() {
        return integer(HE_MANA_COST, 200);
    }

    public static int heCooldownSeconds() {
        return integer(HE_COOLDOWN_SECONDS, 45);
    }

    public static double heSpeed() {
        return real(HE_SPEED, 0.95D);
    }

    public static double heMaxDistance() {
        return real(HE_MAX_DISTANCE, 40.0D);
    }

    public static int heMaxLifeTicks() {
        return secondsToTicks(HE_MAX_LIFE_SECONDS, 6.0D, 5);
    }

    public static float heDamageAmount() {
        return (float) real(HE_DAMAGE_AMOUNT, 30.0D);
    }

    public static double heHitRadius() {
        return real(HE_HIT_RADIUS, 2.5D);
    }

    public static int heBreakRadius() {
        return integer(HE_BREAK_RADIUS, 1);
    }

    public static boolean heBreakBlocks() {
        return toggle(HE_BREAK_BLOCKS, true);
    }

    public static double heTearLength() {
        return real(HE_TEAR_LENGTH, 8.0D);
    }

    public static double heTearRadius() {
        return real(HE_TEAR_RADIUS, 4.5D);
    }

    public static double heBlastRadius() {
        return real(HE_BLAST_RADIUS, 6.0D);
    }

    public static int muryokushoTicks() {
        return secondsToTicks(MURYOKUSHO_DURATION_SECONDS, 1.0D, 1);
    }

    public static int muryokushoCooldownTicks() {
        return integer(MURYOKUSHO_COOLDOWN_SECONDS, 10) * 20;
    }

    private static int secondsToTicks(ForgeConfigSpec.DoubleValue value, double fallbackSeconds, int minTicks) {
        return Math.max(minTicks, (int) Math.round(real(value, fallbackSeconds) * 20.0D));
    }

    private static boolean flag(Map<String, ForgeConfigSpec.BooleanValue> values, String id, boolean fallback) {
        ForgeConfigSpec.BooleanValue value = values.get(id);
        if (value == null) return fallback;
        try {
            return value.get();
        } catch (Exception e) {
            return fallback;
        }
    }

    private TrueNinsConfig() {}
}
