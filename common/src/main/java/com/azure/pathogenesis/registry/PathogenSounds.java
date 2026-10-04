package com.azure.pathogenesis.registry;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public final class PathogenSounds {

    public static final Supplier<SoundEvent> CONTAMINATION_AMBIENT = register("block.contamination.ambient");

    public static final Supplier<SoundEvent> SPORE_PLANT_RATTLE = register("block.spore_plant.rattle");

    public static final Supplier<SoundEvent> SPORE_RELEASE = register("block.spore_plant.release");

    public static final Supplier<SoundEvent> CANISTER_CRACK = register("block.pathogen_source.crack");

    public static final Supplier<SoundEvent> CANISTER_RUPTURE = register("block.pathogen_source.rupture");

    public static final Supplier<SoundEvent> PATHOGEN_LEAK = register("block.pathogen_source.leak");

    public static final Supplier<SoundEvent> OUTBREAK_ECOLOGICAL = register("ambient.outbreak.ecological");

    public static final Supplier<SoundEvent> HOST_COUGH = register("entity.host.cough");

    public static final Supplier<SoundEvent> HOST_HEARTBEAT = register("entity.host.heartbeat");

    public static final Supplier<SoundEvent> BLOODBURST = register("entity.host.bloodburst");

    public static final Supplier<SoundEvent> BLOODBURSTER_AMBIENT = register("entity.bloodburster.ambient");

    public static final Supplier<SoundEvent> BLOODBURSTER_HURT = register("entity.bloodburster.hurt");

    public static final Supplier<SoundEvent> BLOODBURSTER_DEATH = register("entity.bloodburster.death");

    public static final Supplier<SoundEvent> BLOODBURSTER_MATURE = register("entity.bloodburster.mature");

    public static final Supplier<SoundEvent> NEOMORPH_AMBIENT = register("entity.neomorph.ambient");

    public static final Supplier<SoundEvent> NEOMORPH_HURT = register("entity.neomorph.hurt");

    public static final Supplier<SoundEvent> NEOMORPH_DEATH = register("entity.neomorph.death");

    public static final Supplier<SoundEvent> NEOMORPH_ATTACK = register("entity.neomorph.attack");

    public static final Supplier<SoundEvent> NEOMORPH_LEAP = register("entity.neomorph.leap");

    public static final Supplier<SoundEvent> NEOMORPH_SCREECH = register("entity.neomorph.screech");

    private PathogenSounds() {}

    private static Supplier<SoundEvent> register(String name) {
        return Services.REGISTRY.register(
            Registries.SOUND_EVENT,
            name,
            () -> SoundEvent.createVariableRangeEvent(Pathogenesis.id(name))
        );
    }

    public static void init() {}
}
