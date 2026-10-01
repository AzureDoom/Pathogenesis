package com.azure.pathogenesis.platform.services;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public interface IRegistryHelper {

    <T> Supplier<T> register(ResourceKey<? extends Registry<? super T>> registry, String name, Supplier<T> factory);

    <E extends Mob> Supplier<Item> registerSpawnEgg(
        String name,
        Supplier<EntityType<E>> type,
        int primaryColor,
        int secondaryColor
    );
}
