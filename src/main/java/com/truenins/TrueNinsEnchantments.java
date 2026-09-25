package com.truenins;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TrueNinsEnchantments {

    public record Def(String id, String tagKey, int maxLevel, boolean armor,
                      Enchantment.Rarity rarity, int costBase, int costStep) {}

    public static final List<Def> DEFS = List.of(

        new Def("true_name",  "true_me",    1, false, Enchantment.Rarity.VERY_RARE, 25, 0),
        new Def("stasis",     "muryokusho",     1, false, Enchantment.Rarity.RARE,      20, 0),
        new Def("antiheal",   "antiheal",   1, false, Enchantment.Rarity.RARE,      20, 0),
        new Def("sanction",   "justice",    1, false, Enchantment.Rarity.VERY_RARE, 25, 0),
        new Def("scaling",    "scaling",    1, false, Enchantment.Rarity.RARE,      20, 0),
        new Def("malice",     "malice",     1, false, Enchantment.Rarity.VERY_RARE, 25, 0),
        new Def("blood_rage", "rage",       1, false, Enchantment.Rarity.RARE,      20, 0),

        new Def("divinity",   "deity",      1, true,  Enchantment.Rarity.VERY_RARE, 30, 0),
        new Def("skyward",    "fly",        1, true,  Enchantment.Rarity.VERY_RARE, 30, 0),
        new Def("purity",     "immunity",   1, true,  Enchantment.Rarity.RARE,      20, 0),
        new Def("adaptation", "adaptation", 10, true, Enchantment.Rarity.RARE,      15, 9),
        new Def("mitigation", "mitigation", 1, true,  Enchantment.Rarity.VERY_RARE, 25, 0),
        new Def("revival",    "revive",     5, true,  Enchantment.Rarity.VERY_RARE, 25, 9),
        new Def("last_stand", "resolve",    1, true,  Enchantment.Rarity.VERY_RARE, 30, 0),
        new Def("infinity",   "infinity",   1, true,  Enchantment.Rarity.VERY_RARE, 30, 0)
    );

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
        DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, TrueNinsMod.MODID);

    private static final Map<String, RegistryObject<Enchantment>> BY_TAG = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<Enchantment>> BY_ID = new LinkedHashMap<>();

    static {
        for (Def def : DEFS) {
            RegistryObject<Enchantment> ro = ENCHANTMENTS.register(def.id(), () -> new TNEnchantment(def));
            BY_TAG.put(def.tagKey(), ro);
            BY_ID.put(def.id(), ro);
        }
    }

    private TrueNinsEnchantments() {}

    public static RegistryObject<Enchantment> byTag(String tagKey) {
        return BY_TAG.get(tagKey);
    }

    public static Def defOf(String tagKey) {
        for (Def def : DEFS) {
            if (def.tagKey().equals(tagKey)) return def;
        }
        return null;
    }

    public static int levelOf(ItemStack stack, String tagKey) {
        if (stack == null || stack.isEmpty()) return 0;
        RegistryObject<Enchantment> ro = BY_TAG.get(tagKey);
        if (ro == null || !ro.isPresent()) return 0;
        try {

            return stack.getEnchantmentLevel(ro.get());
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static int bestLevel(LivingEntity entity, String tagKey) {
        if (entity == null) return 0;
        int best = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
                int level = levelOf(entity.getItemBySlot(slot), tagKey);
                if (level > best) best = level;
            } catch (Exception ignored) {
            }
        }
        return best;
    }

    public static class TNEnchantment extends Enchantment {

        private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        private static final EquipmentSlot[] WEAPON_SLOTS = { EquipmentSlot.MAINHAND };

        private final Def def;

        TNEnchantment(Def def) {
            super(def.rarity(),
                  def.armor() ? EnchantmentCategory.ARMOR : EnchantmentCategory.WEAPON,
                  def.armor() ? ARMOR_SLOTS : WEAPON_SLOTS);
            this.def = def;
        }

        public String tagKey() {
            return def.tagKey();
        }

        @Override
        public int getMaxLevel() {
            return def.maxLevel();
        }

        @Override
        public int getMinCost(int level) {
            return def.costBase() + (level - 1) * def.costStep();
        }

        @Override
        public int getMaxCost(int level) {
            return getMinCost(level) + 25 + def.costStep() * 3;
        }

        @Override
        public boolean isDiscoverable() {
            return TrueNinsConfig.enchantDiscoverable(def.id());
        }

        @Override
        public boolean isTradeable() {
            return TrueNinsConfig.enchantTradeable(def.id());
        }

        @Override
        public boolean isTreasureOnly() {
            return TrueNinsConfig.enchantTreasureOnly(def.id());
        }
    }
}
