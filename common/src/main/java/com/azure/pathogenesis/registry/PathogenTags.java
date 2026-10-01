package com.azure.pathogenesis.registry;

import com.azure.pathogenesis.Pathogenesis;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class PathogenTags {

    private PathogenTags() {}

    public static final class Blocks {

        public static final TagKey<Block> CONTAMINATABLE = tag("contaminatable");

        public static final TagKey<Block> CONTAMINATABLE_SOIL = tag("contaminatable_soil");

        public static final TagKey<Block> CONTAMINATABLE_PLANTS = tag("contaminatable_plants");

        public static final TagKey<Block> CONTAMINATED_SOIL = tag("contaminated_soil");

        public static final TagKey<Block> PATHOGEN_GROWTH = tag("pathogen_growth");

        public static final TagKey<Block> SPORE_PLANTS = tag("spore_plants");

        public static final TagKey<Block> CONTAMINATED = tag("contaminated");

        public static final TagKey<Block> STERILIZABLE = tag("sterilizable");

        public static final TagKey<Block> PATHOGEN_IMMUNE = tag("pathogen_immune");

        private Blocks() {}

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, Pathogenesis.id(name));
        }
    }

    public static final class Entities {

        public static final TagKey<EntityType<?>> VALID_HOSTS = tag("valid_hosts");

        public static final TagKey<EntityType<?>> SPORE_IMMUNE = tag("spore_immune");

        public static final TagKey<EntityType<?>> PATHOGEN_IMMUNE = tag("pathogen_immune");

        public static final TagKey<EntityType<?>> NEOMORPH_TARGETS = tag("neomorph_targets");

        public static final TagKey<EntityType<?>> BLOODBURSTER_PREY = tag("bloodburster_prey");

        public static final TagKey<EntityType<?>> ALIEN_ORGANISMS = tag("alien_organisms");

        private Entities() {}

        private static TagKey<EntityType<?>> tag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, Pathogenesis.id(name));
        }
    }

    public static final class Items {

        public static final TagKey<Item> SPORE_FILTERS = TagKey.create(
            Registries.ITEM,
            Pathogenesis.id("spore_filters")
        );

        private Items() {}
    }
}
