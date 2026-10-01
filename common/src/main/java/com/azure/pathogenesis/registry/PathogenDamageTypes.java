package com.azure.pathogenesis.registry;

import com.azure.pathogenesis.Pathogenesis;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class PathogenDamageTypes {

    public static final ResourceKey<DamageType> PATHOGEN = key("pathogen");

    public static final ResourceKey<DamageType> BLOODBURST = key("bloodburst");

    private PathogenDamageTypes() {}

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Pathogenesis.id(name));
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }
}
