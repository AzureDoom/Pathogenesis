package com.azure.pathogenesis.worldgen;

import com.azure.pathogenesis.registry.PathogenStructureTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class DryLandStructure extends Structure {

    public static final MapCodec<DryLandStructure> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            settingsCodec(instance),
            Structure.DIRECT_CODEC.fieldOf("structure").forGetter(s -> s.delegate),
            Codec.intRange(1, 64).optionalFieldOf("check_radius", 13).forGetter(s -> s.checkRadius),
            Codec.intRange(0, 64).optionalFieldOf("max_height_difference", 4).forGetter(s -> s.maxHeightDifference)
        ).apply(instance, DryLandStructure::new)
    );

    private final Structure delegate;

    private final int checkRadius;

    private final int maxHeightDifference;

    public DryLandStructure(
        StructureSettings settings,
        Structure delegate,
        int checkRadius,
        int maxHeightDifference
    ) {
        super(settings);
        this.delegate = delegate;
        this.checkRadius = checkRadius;
        this.maxHeightDifference = maxHeightDifference;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        return delegate.findValidGenerationPoint(context).filter(stub -> isDryAndFlat(context, stub.position()));
    }

    private boolean isDryAndFlat(GenerationContext context, BlockPos center) {
        var generator = context.chunkGenerator();
        var heightAccessor = context.heightAccessor();
        var randomState = context.randomState();
        var min = Integer.MAX_VALUE;
        var max = Integer.MIN_VALUE;
        for (var dx = -checkRadius; dx <= checkRadius; dx += checkRadius) {
            for (var dz = -checkRadius; dz <= checkRadius; dz += checkRadius) {
                var x = center.getX() + dx;
                var z = center.getZ() + dz;
                var surface = generator.getFirstOccupiedHeight(
                    x,
                    z,
                    Heightmap.Types.WORLD_SURFACE_WG,
                    heightAccessor,
                    randomState
                );
                var ground = generator.getFirstOccupiedHeight(
                    x,
                    z,
                    Heightmap.Types.OCEAN_FLOOR_WG,
                    heightAccessor,
                    randomState
                );
                if (surface != ground) {
                    return false;
                }
                min = Math.min(min, ground);
                max = Math.max(max, ground);
            }
        }
        return max - min <= maxHeightDifference;
    }

    @Override
    public @NotNull StructureType<?> type() {
        return PathogenStructureTypes.DRY_LAND.get();
    }
}
