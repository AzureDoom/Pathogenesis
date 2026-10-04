package com.azure.pathogenesis.item;

import com.azure.pathogenesis.entity.ThrownPathogenAmpule;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PathogenAmpuleItem extends Item implements ProjectileItem {

    public PathogenAmpuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level,
        @NotNull Player player,
        @NotNull InteractionHand hand
    ) {
        var stack = player.getItemInHand(hand);
        level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.SPLASH_POTION_THROW,
            SoundSource.PLAYERS,
            0.5F,
            0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
        );
        if (!level.isClientSide()) {
            var ampule = new ThrownPathogenAmpule(level, player);
            ampule.setItem(stack);
            ampule.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
            level.addFreshEntity(ampule);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, 20);
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public @NotNull Projectile asProjectile(
        @NotNull Level level,
        @NotNull Position pos,
        @NotNull ItemStack stack,
        @NotNull Direction direction
    ) {
        var ampule = new ThrownPathogenAmpule(level, pos.x(), pos.y(), pos.z());
        ampule.setItem(stack);
        return ampule;
    }

    @Override
    public @NotNull DispenseConfig createDispenseConfig() {
        return DispenseConfig.builder()
            .uncertainty(DispenseConfig.DEFAULT.uncertainty() * 0.5F)
            .power(
                DispenseConfig.DEFAULT.power() * 1.25F
            )
            .build();
    }

    @Override
    public void onDestroyed(@NotNull ItemEntity itemEntity) {
        if (itemEntity.level() instanceof ServerLevel level) {
            var source = itemEntity instanceof AmpuleDamageTracker tracker ? tracker.pathogenesis$lastDamage() : null;
            AmpuleHazards.onDestroyed(level, itemEntity, source);
        }
        super.onDestroyed(itemEntity);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.add(
            Component.translatable("item.pathogenesis.sealed_pathogen_ampule.tooltip")
                .withStyle(ChatFormatting.DARK_RED)
        );
        tooltip.add(
            Component.translatable("item.pathogenesis.sealed_pathogen_ampule.fragile")
                .withStyle(ChatFormatting.GRAY)
        );
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
