package com.azure.pathogenesis.registry;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.entity.BloodbursterEntity;
import com.azure.pathogenesis.entity.NeomorphEntity;
import com.azure.pathogenesis.entity.NeophyteEntity;
import com.azure.pathogenesis.entity.SporeCloudEntity;
import com.azure.pathogenesis.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class PathogenEntities {

    public static final Supplier<EntityType<SporeCloudEntity>> SPORE_CLOUD = register(
        "spore_cloud",
        () -> EntityType.Builder.<SporeCloudEntity>of(SporeCloudEntity::new, MobCategory.MISC)
            .sized(2.0F, 1.2F)
            .fireImmune()
            .clientTrackingRange(8)
            .updateInterval(10)
            .build(Pathogenesis.id("spore_cloud").toString())
    );

    public static final Supplier<EntityType<BloodbursterEntity>> BLOODBURSTER = register(
        "bloodburster",
        () -> EntityType.Builder.of(BloodbursterEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 0.5F)
            .clientTrackingRange(8)
            .build(Pathogenesis.id("bloodburster").toString())
    );

    public static final Supplier<EntityType<NeophyteEntity>> NEOPHYTE = register(
        "neophyte",
        () -> EntityType.Builder.of(NeophyteEntity::new, MobCategory.MONSTER)
            .sized(0.7F, 1.6F)
            .clientTrackingRange(10)
            .build(Pathogenesis.id("neophyte").toString())
    );

    public static final Supplier<EntityType<NeomorphEntity>> NEOMORPH = register(
        "neomorph",
        () -> EntityType.Builder.of(NeomorphEntity::new, MobCategory.MONSTER)
            .sized(0.9F, 2.4F)
            .clientTrackingRange(10)
            .build(Pathogenesis.id("neomorph").toString())
    );

    private PathogenEntities() {}

    private static <T extends net.minecraft.world.entity.Entity> Supplier<EntityType<T>> register(
        String name,
        Supplier<EntityType<T>> factory
    ) {
        return Services.REGISTRY.register(Registries.ENTITY_TYPE, name, factory);
    }

    public static void registerAttributes(BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier> sink) {
        sink.accept(BLOODBURSTER.get(), BloodbursterEntity.createAttributes().build());
        sink.accept(NEOPHYTE.get(), NeophyteEntity.createAttributes().build());
        sink.accept(NEOMORPH.get(), NeomorphEntity.createAttributes().build());
    }

    public static void init() {}
}
