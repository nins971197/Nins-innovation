package com.truenins.compat.irons;

import com.truenins.TrueNinsConfig;
import com.truenins.TrueNinsMod;
import com.truenins.register.TNMobEffects;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class MadeInHeavenSpell extends AbstractSpell {

    private final DefaultConfig defaultConfig = new DefaultConfig()
        .setMinRarity(SpellRarity.LEGENDARY)
        .setSchoolResource(TNIronsCompat.CURSE_ID)
        .setMaxLevel(1)
        .setCooldownSeconds(120.0D)
        .setAllowCrafting(false)
        .build();

    public MadeInHeavenSpell() {
        this.castTime = TrueNinsConfig.madeInHeavenDurationTicks();
    }

    @Override
    public ResourceLocation getSpellResource() {
        return new ResourceLocation(TrueNinsMod.MODID, "made_in_heaven");
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public SchoolType getSchoolType() {
        return TNIronsCompat.CURSE.get();
    }

    @Override
    public int getManaCost(int spellLevel) {
        int perSecond = TrueNinsConfig.madeInHeavenManaPerSecond();
        if (perSecond <= 0) return 0;
        return Math.max(1, perSecond / 2);
    }

    @Override
    public int getCastTime(int spellLevel) {
        return TrueNinsConfig.madeInHeavenDurationTicks();
    }

    @Override
    public int getSpellCooldown() {
        return TrueNinsConfig.madeInHeavenCooldownSeconds() * 20;
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public SpellRarity getRarity(int spellLevel) {
        return SpellRarity.LEGENDARY;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData magicData) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;
        player.addEffect(new MobEffectInstance(
            TNMobEffects.MADE_IN_HEAVEN.get(), 24, 0, false, false, false));
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData magicData) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        long step = Math.max(1L, TrueNinsConfig.madeInHeavenTimeTicksPerSecond() / 20L);
        serverLevel.setDayTime(serverLevel.getDayTime() + step);
        ClientboundSetTimePacket timePacket = new ClientboundSetTimePacket(
            serverLevel.getGameTime(), serverLevel.getDayTime(), true);
        for (ServerPlayer viewer : serverLevel.players()) {
            viewer.connection.send(timePacket);
        }

        accelerateNearby(serverLevel, player);
    }

    @SuppressWarnings("unchecked")
    private static void accelerateNearby(ServerLevel level, ServerPlayer caster) {
        if (!TrueNinsConfig.madeInHeavenAccelerateWorld()) return;

        int radius = TrueNinsConfig.madeInHeavenRadius();
        if (radius <= 0) return;

        List<Entity> entities = level.getEntities(caster,
            caster.getBoundingBox().inflate(radius + 1.0D),
            e -> !(e instanceof Player) && !e.isPassenger() && e.isAlive());
        for (Entity mover : entities) {
            if (mover.isRemoved()) continue;
            level.tickNonPassenger(mover);
        }

        BlockPos center = caster.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int radiusSqr = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSqr) continue;
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);

                    BlockState state = level.getBlockState(cursor);
                    if (state.isRandomlyTicking()) {
                        state.randomTick(level, cursor, level.random);
                        state = level.getBlockState(cursor);
                    }
                    if (!state.hasBlockEntity()) continue;

                    BlockEntity blockEntity = level.getBlockEntity(cursor);
                    if (blockEntity == null || blockEntity.isRemoved()) continue;

                    BlockEntityTicker<BlockEntity> ticker = state.getTicker(level,
                        (BlockEntityType<BlockEntity>) blockEntity.getType());
                    if (ticker != null) ticker.tick(level, cursor, state, blockEntity);
                }
            }
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
            Component.translatable("ui.truenins.made_in_heaven.mana", TrueNinsConfig.madeInHeavenManaPerSecond()),
            Component.translatable("ui.truenins.made_in_heaven.duration", TrueNinsConfig.madeInHeavenDurationTicks() / 20),
            Component.translatable("ui.truenins.made_in_heaven.radius", TrueNinsConfig.madeInHeavenRadius()));
    }
}
