package com.azure.pathogenesis.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public class ReplaceWithItemModifier extends LootModifier {

    public static final MapCodec<ReplaceWithItemModifier> CODEC = RecordCodecBuilder.mapCodec(
        instance -> codecStart(instance).and(
            instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m -> m.item),
                Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(m -> m.chance)
            )
        ).apply(instance, ReplaceWithItemModifier::new)
    );

    private final Item item;

    private final float chance;

    public ReplaceWithItemModifier(LootItemCondition[] conditions, Item item, float chance) {
        super(conditions);
        this.item = item;
        this.chance = chance;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(
        @NotNull ObjectArrayList<ItemStack> loot,
        LootContext context
    ) {
        if (context.getRandom().nextFloat() < chance) {
            loot.clear();
            loot.add(new ItemStack(item));
        }
        return loot;
    }

    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
