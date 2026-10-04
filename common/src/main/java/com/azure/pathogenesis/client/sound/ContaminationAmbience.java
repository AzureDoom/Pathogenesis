package com.azure.pathogenesis.client.sound;

import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class ContaminationAmbience {

    private static ContaminationLoopSound current;

    private static float intensity;

    private ContaminationAmbience() {}

    public static float intensity() {
        return intensity;
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            if (mc.level == null) {
                intensity = 0.0F;
                current = null;
            }
            return;
        }
        if (mc.level.getGameTime() % 20 == 0) {
            intensity = sample(mc.level, mc.player.blockPosition());
        }
        if (intensity > 0.0F && (current == null || current.isStopped() || !mc.getSoundManager().isActive(current))) {
            current = new ContaminationLoopSound();
            mc.getSoundManager().play(current);
        }
    }

    private static float sample(Level level, BlockPos center) {
        var pos = new BlockPos.MutableBlockPos();
        var hits = 0;
        var total = 0;
        for (var x = -12; x <= 12; x += 3) {
            for (var z = -12; z <= 12; z += 12) {
                for (var y = -3; y <= 1; y++) {
                    total++;
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    var state = level.getBlockState(pos);
                    if (
                        state.is(PathogenTags.Blocks.CONTAMINATED)
                            && !state.is(PathogenTags.Blocks.DORMANT_CONTAMINATION)
                    ) {
                        hits++;
                    }
                }
            }
        }
        return Mth.clamp(hits / (float) total / 0.30F, 0.0F, 1.0F);
    }
}
