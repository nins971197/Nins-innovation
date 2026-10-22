package com.truenins.spell.domain;

import com.truenins.TrueNinsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Target classification is shared by targeting, the trial and the integration checks. */
public final class JudgmentRules {
    public enum Kind { INNOCENT, HOSTILE, BOSS, PLAYER }
    public static final int UNDECIDED = 0, ACQUITTED = 1, CONFISCATION = 2, DEATH = 3;
    private JudgmentRules() {}

    public static Kind kind(LivingEntity entity) {
        if (entity instanceof Player) return Kind.PLAYER;
        if (entity instanceof WitherBoss || entity instanceof EnderDragon || tagged(entity, TrueNinsMod.MODID, "judgment_bosses")
                || tagged(entity, "forge", "bosses") || tagged(entity, "c", "bosses")) return Kind.BOSS;
        return entity instanceof Enemy ? Kind.HOSTILE : Kind.INNOCENT;
    }

    private static boolean tagged(LivingEntity entity, String namespace, String path) {
        return entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(namespace, path)));
    }
}
