package com.azure.pathogenesis.neoforge.platform;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.platform.services.IRegistryHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class NeoForgeRegistryHelper implements IRegistryHelper {

    private static final Map<ResourceKey<?>, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();

    public static void attach(IEventBus modBus) {
        REGISTERS.values().forEach(register -> register.register(modBus));
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> Supplier<T> register(
        ResourceKey<? extends Registry<? super T>> registryKey,
        String name,
        Supplier<T> factory
    ) {
        DeferredRegister register = REGISTERS.computeIfAbsent(
            registryKey,
            key -> DeferredRegister.create((ResourceKey) key, Pathogenesis.MOD_ID)
        );
        return (Supplier<T>) register.register(name, factory);
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
            () -> new DeferredSpawnEggItem(type, primary, secondary, new Item.Properties())
        );
    }
}
