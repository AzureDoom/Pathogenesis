package com.azure.pathogenesis.item;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.contamination.RuptureStrength;
import com.azure.pathogenesis.registry.PathogenItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AmpuleHazards {

    private static final double CHAIN_RADIUS = 3.0D;

    private AmpuleHazards() {}

    public static boolean isAmpule(ItemStack stack) {
        return !stack.isEmpty() && stack.is(PathogenItems.SEALED_PATHOGEN_AMPULE.get());
    }

    public static void onDestroyed(ServerLevel level, ItemEntity entity, @Nullable DamageSource source) {
        if (entity.isRemoved()) {
            return;
        }
        var fragile = Pathogenesis.getConfig().ampuleConfigs.fragileAmpules;
        if (fragile && (entity.isInLava() || source != null && source.is(DamageTypeTags.IS_FIRE))) {
            incinerate(level, entity.position());
            return;
        }
        if (fragile && source != null && source.is(DamageTypeTags.IS_EXPLOSION)) {
            explode(level, entity);
            return;
        }
        PathogenZoneManager.onRupture(level, entity.blockPosition(), RuptureStrength.RUPTURE, false);
    }

    private static void incinerate(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 0.2D, pos.z, 8, 0.15D, 0.1D, 0.15D, 0.02D);
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.2D, pos.z, 4, 0.1D, 0.1D, 0.1D, 0.01D);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.5F, 1.4F);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    private static void explode(ServerLevel level, ItemEntity origin) {
        var count = origin.getItem().getCount();
        var positions = new ArrayList<Vec3>();
        positions.add(origin.position());
        for (
            var other : level.getEntitiesOfClass(
                ItemEntity.class,
                origin.getBoundingBox().inflate(CHAIN_RADIUS),
                item -> item != origin && item.isAlive() && isAmpule(item.getItem())
            )
        ) {
            count += other.getItem().getCount();
            positions.add(other.position());
            other.discard();
        }
        var bursts = Math.min(count, Pathogenesis.getConfig().ampuleConfigs.maxExplosionBursts);
        burstScattered(level, positions, bursts);
    }

    private static void burstScattered(ServerLevel level, List<Vec3> anchors, int bursts) {
        var random = level.random;
        for (var i = 0; i < bursts; i++) {
            var anchor = anchors.get(i % anchors.size());
            var pos = BlockPos.containing(
                anchor.x + (random.nextDouble() - 0.5D) * 3.0D,
                anchor.y + random.nextDouble(),
                anchor.z + (random.nextDouble() - 0.5D) * 3.0D
            );
            PathogenZoneManager.onRupture(
                level,
                pos,
                i == 0 ? RuptureStrength.EXPLOSION : RuptureStrength.RUPTURE,
                false
            );
        }
    }

    public static void onLanded(ServerLevel level, ItemEntity entity, float fallDistance) {
        var stack = entity.getItem();
        if (!isAmpule(stack)) {
            return;
        }
        var config = Pathogenesis.getConfig().ampuleConfigs;
        if (!config.fragileAmpules || fallDistance < config.fallRuptureHeight) {
            return;
        }
        if (isSoftLanding(level.getBlockState(entity.getOnPos()))) {
            return;
        }
        var chance = Math.min(
            config.fallRuptureMaxChance,
            config.fallRuptureBaseChance + config.fallRupturePerBlock * (fallDistance - config.fallRuptureHeight)
        );
        if (level.random.nextDouble() >= chance) {
            level.playSound(null, entity.blockPosition(), SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.7F, 1.6F);
            return;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(stack);
        }
        level.playSound(null, entity.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        PathogenZoneManager.onRupture(level, entity.blockPosition(), RuptureStrength.RUPTURE, false);
    }

    private static boolean isSoftLanding(BlockState state) {
        return state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS) || state.is(BlockTags.BEDS)
            || state.is(BlockTags.SNOW) || state.is(Blocks.HAY_BLOCK) || state.is(Blocks.SLIME_BLOCK)
            || state.is(Blocks.HONEY_BLOCK) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MOSS_CARPET);
    }

    public static void onCarrierHurt(LivingEntity entity, DamageSource source, float amount) {
        var config = Pathogenesis.getConfig().ampuleConfigs;
        if (
            !config.fragileAmpules || !config.carriedAmpulesCanBreak || !(entity.level() instanceof ServerLevel level)
        ) {
            return;
        }
        double chance;
        if (source.is(DamageTypeTags.IS_FALL)) {
            if (amount < config.carriedFallDamageThreshold) {
                return;
            }
            chance = Math.min(
                config.fallRuptureMaxChance,
                config.fallRuptureBaseChance + config.fallRupturePerBlock * (amount - config.carriedFallDamageThreshold)
            );
        } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            chance = config.carriedExplosionBreakChance;
        } else {
            return;
        }
        if (chance <= 0.0D) {
            return;
        }
        var random = level.random;
        for (var stack : carriedStacks(entity)) {
            if (isAmpule(stack) && random.nextDouble() < chance) {
                stack.shrink(1);
                level.playSound(null, entity.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.9F);
                PathogenZoneManager.onRupture(level, entity.blockPosition(), RuptureStrength.RUPTURE, false);
                return;
            }
        }
    }

    private static List<ItemStack> carriedStacks(LivingEntity entity) {
        if (entity instanceof Player player) {
            var inventory = player.getInventory();
            var stacks = new ArrayList<ItemStack>(inventory.items.size() + inventory.offhand.size());
            stacks.addAll(inventory.items);
            stacks.addAll(inventory.offhand);
            return stacks;
        }
        return List.of(entity.getItemBySlot(EquipmentSlot.MAINHAND), entity.getItemBySlot(EquipmentSlot.OFFHAND));
    }
}
