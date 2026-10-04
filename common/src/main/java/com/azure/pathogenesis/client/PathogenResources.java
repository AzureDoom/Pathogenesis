package com.azure.pathogenesis.client;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.contamination.ContainmentState;
import net.minecraft.resources.ResourceLocation;

public final class PathogenResources {

    public static final ResourceLocation SOURCE_GEO = geoLocation(Folder.BLOCK, "pathogen_source");

    public static final ResourceLocation SOURCE_ANIMATION = animationLocation(Folder.BLOCK, "pathogen_source");

    private static final ResourceLocation SOURCE_SEALED = textureLocation(Folder.BLOCK, "pathogen_source_sealed");

    private static final ResourceLocation SOURCE_DAMAGED = textureLocation(Folder.BLOCK, "pathogen_source_damaged");

    private static final ResourceLocation SOURCE_LEAKING = textureLocation(Folder.BLOCK, "pathogen_source_leaking");

    public static final ModelAssets BLOODBURSTER = ModelAssets.of(Folder.ENTITY, "bloodburster");

    public static final ModelAssets NEOPHYTE = ModelAssets.of(Folder.ENTITY, "neophyte");

    public static final ModelAssets NEOMORPH = ModelAssets.of(Folder.ENTITY, "neomorph");

    public static final ModelAssets SPOREPODS = ModelAssets.of(Folder.BLOCK, "sporepods");

    public static final ModelAssets SEALED_PATHOGEN_AMPULE = ModelAssets.of(Folder.ITEM, "sealed_pathogen_ampule");

    private PathogenResources() {}

    public static ResourceLocation sourceTexture(ContainmentState state) {
        return switch (state) {
            case SEALED, OPEN, EMPTY -> SOURCE_SEALED;
            case DAMAGED -> SOURCE_DAMAGED;
            case LEAKING -> SOURCE_LEAKING;
        };
    }

    private static ResourceLocation geoLocation(Folder folder, String name) {
        return Pathogenesis.id("geo/" + folder.path + "/" + name + ".geo.json");
    }

    private static ResourceLocation animationLocation(Folder folder, String name) {
        return Pathogenesis.id("animations/" + folder.path + "/" + name + ".animation.json");
    }

    private static ResourceLocation textureLocation(Folder folder, String name) {
        return Pathogenesis.id("textures/" + folder.path + "/" + name + ".png");
    }

    public record ModelAssets(
        ResourceLocation geo,
        ResourceLocation animation,
        ResourceLocation texture
    ) {

        static ModelAssets of(Folder folder, String name) {
            return new ModelAssets(
                geoLocation(folder, name),
                animationLocation(folder, name),
                textureLocation(folder, name)
            );
        }
    }

    private enum Folder {

        ENTITY("entity"),
        BLOCK("block"),
        ITEM("item");

        final String path;

        Folder(String path) {
            this.path = path;
        }
    }
}
