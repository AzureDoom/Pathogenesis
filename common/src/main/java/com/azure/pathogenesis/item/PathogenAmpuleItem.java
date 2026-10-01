package com.azure.pathogenesis.item;

import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.contamination.RuptureStrength;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PathogenAmpuleItem extends BlockItem {

    public PathogenAmpuleItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        if (itemEntity.level() instanceof ServerLevel level) {
            PathogenZoneManager.onRupture(level, itemEntity.blockPosition(), RuptureStrength.RUPTURE, false);
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
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
