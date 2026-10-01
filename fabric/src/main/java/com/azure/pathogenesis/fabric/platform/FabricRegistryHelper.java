package com.azure.pathogenesis.fabric.platform;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.platform.services.IRegistryHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.function.Supplier;

public final class FabricRegistryHelper implements IRegistryHelper {

    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<T> register(
        ResourceKey<? extends Registry<? super T>> registryKey,
        String name,
        Supplier<T> factory
    ) {
        Registry<? super T> registry = (Registry<? super T>) BuiltInRegistries.REGISTRY.get(registryKey.location());
        if (registry == null) {
            throw new IllegalStateException("Unknown registry " + registryKey.location());
        }
        T value = Registry.register(registry, Pathogenesis.id(name), factory.get());
        return () -> value;
    }

    @Override
    public <E extends Mob> Supplier<Item> registerSpawnEgg(
        String name,
        Supplier<EntityType<E>> type,
        int primary,
        int secondary
    ) {
        return register(
            Registries.ITEM,
            name,
            () -> new SpawnEggItem(type.get(), primary, secondary, new Item.Properties())
        );
    }
}
