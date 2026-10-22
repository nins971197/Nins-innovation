package com.truenins.gameplay.tag;

import com.truenins.gameplay.enchantment.TrueNinsEnchantments;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Channel aware keyword lookup.
 *
 * <p>A truenins keyword can be carried by two independent sources: NBT/scoreboard
 * markers (channel {@link #NBT}) and the same-name enchantment (channel
 * {@link #ENCHANT}). Both channels are tracked separately so trackers can keep
 * one cooldown and one parameter set per source instead of collapsing into a
 * single boolean.</p>
 *
 * <p>Entity lookups are memoised per game tick: one equipment scan answers every
 * keyword question for that entity in that tick. Call {@link #invalidate} when
 * the equipment or tags change before the next tick.</p>
 */
public final class TrueninsChannels {

    public static final int NBT = 0;
    public static final int ENCHANT = 1;

    public static final int NBT_BIT = 1;
    public static final int ENCHANT_BIT = 1 << 1;

    /** Legacy keyword aliases kept readable-only for worlds created before the stun rename. */
    private static final Map<String, String> LEGACY_ALIAS = Map.of("stun", "muryokusho");

    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();

    private static final String[] KEYS;
    private static final String[] LEGACY_KEYS;
    private static final int KEY_COUNT;

    private static final Map<String, Integer> INDEX = new HashMap<>();
    /** Reverse alias table, so a marker lookup only visits the keys the tag actually holds. */
    private static final Map<String, Integer> LEGACY_INDEX = new HashMap<>();
    private static final Map<UUID, Snapshot> CACHE = new ConcurrentHashMap<>();

    private static final int CACHE_LIMIT = 4096;

    static {
        List<String> keys = TrueNinsTags.KEYS;
        KEY_COUNT = keys.size();
        KEYS = keys.toArray(new String[0]);
        LEGACY_KEYS = new String[KEY_COUNT];
        for (int i = 0; i < KEY_COUNT; i++) {
            INDEX.put(KEYS[i], i);
            LEGACY_KEYS[i] = LEGACY_ALIAS.get(KEYS[i]);
            if (LEGACY_KEYS[i] != null) LEGACY_INDEX.put(LEGACY_KEYS[i], i);
        }
    }

    private static final class Snapshot {
        long tick = Long.MIN_VALUE;
        int entityTicks;
        boolean clientSide;
        int nbtMask;
        int enchMask;
        int[] levels;
    }

    private TrueninsChannels() {}

    public static int bit(int channel) {
        return 1 << channel;
    }

    /** Legacy (pre-rename) marker name of {@code key}, or {@code null}. Read-only compatibility. */
    public static String legacyAlias(String key) {
        return key == null ? null : LEGACY_ALIAS.get(key);
    }

    /** Channel mask of one entity: {@link #NBT_BIT} for NBT markers, {@link #ENCHANT_BIT} for the enchantment. */
    public static int mask(LivingEntity entity, String key) {
        if (entity == null) return 0;
        int index = indexOf(key);
        if (index < 0) return 0;
        Snapshot snapshot = snapshot(entity);
        int mask = 0;
        if (((snapshot.nbtMask >>> index) & 1) != 0) mask |= NBT_BIT;
        if (((snapshot.enchMask >>> index) & 1) != 0) mask |= ENCHANT_BIT;
        return mask;
    }

    public static boolean has(LivingEntity entity, String key) {
        return mask(entity, key) != 0;
    }

    public static boolean has(LivingEntity entity, String key, int channel) {
        return (mask(entity, key) & bit(channel)) != 0;
    }

    /** Highest enchantment level of {@code key} worn or held by the entity, 0 when absent. */
    public static int level(LivingEntity entity, String key) {
        if (entity == null) return 0;
        int index = indexOf(key);
        if (index < 0) return 0;
        Snapshot snapshot = snapshot(entity);
        if (((snapshot.enchMask >>> index) & 1) == 0) return 0;
        int[] levels = snapshot.levels;
        return levels == null ? 0 : levels[index];
    }

    /** Channel mask of a single stack, used by weapon keywords such as stun and antiheal. */
    public static int itemMask(ItemStack stack, String key) {
        if (stack == null || stack.isEmpty()) return 0;
        int index = indexOf(key);
        if (index < 0) return 0;
        int mask = 0;
        CompoundTag nbt = stack.getTag();
        if (nbt != null && (nbt.getBoolean(KEYS[index])
                || (LEGACY_KEYS[index] != null && nbt.getBoolean(LEGACY_KEYS[index])))) {
            mask |= NBT_BIT;
        }
        if (stack.isEnchanted() && TrueNinsEnchantments.levelOf(stack, key) > 0) {
            mask |= ENCHANT_BIT;
        }
        return mask;
    }

    public static int itemLevel(ItemStack stack, String key) {
        return TrueNinsEnchantments.levelOf(stack, key);
    }

    public static void invalidate(LivingEntity entity) {
        if (entity != null) CACHE.remove(entity.getUUID());
    }

    public static void clean(UUID id) {
        if (id != null) CACHE.remove(id);
    }

    private static int indexOf(String key) {
        if (key == null) return -1;
        Integer index = INDEX.get(key);
        return index == null ? -1 : index;
    }

    private static Snapshot snapshot(LivingEntity entity) {
        UUID id = entity.getUUID();
        long tick = entity.level().getGameTime();
        int ticks = entity.tickCount;
        boolean clientSide = entity.level().isClientSide;
        Snapshot snapshot = CACHE.get(id);
        if (snapshot != null && snapshot.tick == tick && snapshot.entityTicks == ticks
                && snapshot.clientSide == clientSide) {
            return snapshot;
        }
        if (snapshot == null) {
            if (CACHE.size() >= CACHE_LIMIT) CACHE.clear();
            snapshot = new Snapshot();
            CACHE.put(id, snapshot);
        }
        snapshot.tick = tick;
        snapshot.entityTicks = ticks;
        snapshot.clientSide = clientSide;
        snapshot.nbtMask = 0;
        snapshot.enchMask = 0;
        if (snapshot.levels != null) java.util.Arrays.fill(snapshot.levels, 0);

        for (String tag : entity.getTags()) {
            String key = tag.startsWith("+") ? tag.substring(1) : tag;
            Integer index = INDEX.get(key);
            if (index == null) index = LEGACY_INDEX.get(key);
            if (index != null) snapshot.nbtMask |= 1 << index;
        }

        snapshot.nbtMask |= markerMask(entity.getPersistentData());

        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack;
            try {
                stack = entity.getItemBySlot(slot);
            } catch (Exception ignored) {
                continue;
            }
            if (stack.isEmpty()) continue;

            snapshot.nbtMask |= markerMask(stack.getTag());
            if (!stack.isEnchanted()) continue;
            scanEnchantments(stack, slot, snapshot);
        }
        return snapshot;
    }

    /**
     * Marker keys held by one tag, matched by visiting the keys the tag actually has
     * instead of probing every known keyword. The pre-rename alias is accepted here.
     */
    private static int markerMask(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) return 0;
        int mask = 0;
        for (String key : nbt.getAllKeys()) {
            Integer index = INDEX.get(key);
            if (index == null) index = LEGACY_INDEX.get(key);
            if (index == null) continue;
            if (nbt.getBoolean(key)) mask |= 1 << index;
        }
        return mask;
    }

    private static void scanEnchantments(ItemStack stack, EquipmentSlot slot, Snapshot snapshot) {
        boolean armorSlot = slot.isArmor();
        if (!armorSlot && slot != EquipmentSlot.MAINHAND) return;
        for (TrueNinsEnchantments.Def def : TrueNinsEnchantments.DEFS) {
            if (def.armor() != armorSlot) continue;
            int level;
            try {
                level = TrueNinsEnchantments.levelOf(stack, def.tagKey());
            } catch (Exception ignored) {
                continue;
            }
            if (level <= 0) continue;
            int index = indexOf(def.tagKey());
            if (index < 0) continue;
            snapshot.enchMask |= 1 << index;
            if (snapshot.levels == null) snapshot.levels = new int[KEY_COUNT];
            if (level > snapshot.levels[index]) snapshot.levels[index] = level;
        }
    }
}
