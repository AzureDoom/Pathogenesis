package com.azure.pathogenesis.item;

import com.azure.pathogenesis.block.PathogenSourceBlock;
import com.azure.pathogenesis.blockentity.PathogenSourceBlockEntity;
import com.azure.pathogenesis.contamination.ContainmentState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

public class PathogenSourceItem extends BlockItem {

    private static final Map<ItemStack, ContainmentState> SENT = new WeakHashMap<>();

    public PathogenSourceItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull Entity entity,
        int slot,
        boolean selected
    ) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!(level instanceof ServerLevel)) {
            return;
        }
        var display = PathogenSourceBlock.itemDisplayState(stack);
        if (SENT.put(stack, display) != display) {
            PathogenSourceBlockEntity.holdCommand(display).sendForItem(entity, stack);
        }
    }
}
