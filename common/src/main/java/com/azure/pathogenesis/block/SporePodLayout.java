package com.azure.pathogenesis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class SporePodLayout {

    public record Pod(
        double x,
        double z,
        float yaw,
        float scale
    ) {}

    private static final VoxelShape[][] SHAPES = new VoxelShape[3][8];

    private SporePodLayout() {}

    public static List<Pod> of(BlockPos pos) {
        var seed = seed(pos);
        var count = count(seed);
        var rotation = rotation(seed);
        var pods = new ArrayList<Pod>(count);
        for (var i = 0; i < count; i++) {
            var yaw = ((seed >>> (24 + 4 * i)) & 15) * 22.5F;
            var scale = 0.85F + ((seed >>> (40 + 3 * i)) & 7) / 7.0F * (float) (1.1D - 0.85D);
            var offset = offset(count, rotation, i);
            pods.add(new Pod(0.5D + offset[0] / 16.0D, 0.5D + offset[1] / 16.0D, yaw, count == 1 ? 1.0F : scale));
        }
        return pods;
    }

    public static VoxelShape shape(BlockPos pos) {
        var seed = seed(pos);
        var count = count(seed);
        var rotation = rotation(seed);
        var cached = SHAPES[count - 1][rotation];
        if (cached != null) {
            return cached;
        }
        var shape = Shapes.empty();
        var half = 3.0D * 1.1D;
        for (var i = 0; i < count; i++) {
            var offset = offset(count, rotation, i);
            var cx = 8.0D + offset[0];
            var cz = 8.0D + offset[1];
            shape = Shapes.or(
                shape,
                Block.box(
                    Math.max(0.0D, cx - half),
                    0.0D,
                    Math.max(0.0D, cz - half),
                    Math.min(16.0D, cx + half),
                    4.0D * 1.1D,
                    Math.min(16.0D, cz + half)
                )
            );
        }
        return SHAPES[count - 1][rotation] = shape.optimize();
    }

    private static int count(long seed) {
        var roll = (seed >>> 8) & 0xFF;
        return roll < 102 ? 1 : roll < 192 ? 2 : 3;
    }

    private static int rotation(long seed) {
        return (int) ((seed >>> 16) & (8 - 1));
    }

    private static double[] offset(int count, int rotation, int index) {
        if (count == 1) {
            return new double[] { 0.0D, 0.0D };
        }
        var radius = count == 2 ? 3.5D : 4.5D;
        var angle = Math.toRadians(rotation * (360.0D / 8) + index * (360.0D / count));
        return new double[] { Math.cos(angle) * radius, Math.sin(angle) * radius };
    }

    private static long seed(BlockPos pos) {
        var z = pos.asLong() + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
