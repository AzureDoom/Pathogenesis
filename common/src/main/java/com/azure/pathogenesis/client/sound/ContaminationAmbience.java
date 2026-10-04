package com.azure.pathogenesis.client.sound;

import com.azure.pathogenesis.client.outbreak.ClientOutbreakState;
import com.azure.pathogenesis.registry.PathogenTags;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

@SuppressWarnings("unused")
public final class ContaminationAmbience {

    private static ContaminationLoopSound current;

    private static float intensity;

    private static float activity = 1.0F;

    private static boolean gateOpen = true;

    private static int gateTimer;

    private ContaminationAmbience() {}

    public static float intensity() {
        return intensity;
    }

    public static float targetIntensity() {
        return gateOpen ? intensity : 0.0F;
    }

    public static float pitch() {
        return 0.8F + 0.2F * activity;
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            if (mc.level == null) {
                intensity = 0.0F;
                activity = 1.0F;
                gateOpen = true;
                gateTimer = 0;
                current = null;
                ClientOutbreakState.clear();
            }
            return;
        }
        if (mc.level.getGameTime() % 20 == 0) {
            var pos = mc.player.blockPosition();
            intensity = sample(mc.level, pos);
            activity = ClientOutbreakState.activityAt(mc.level, pos);
        }
        updateGate(mc.level.getRandom());
        if (
            targetIntensity() > 0.0F
                && (current == null || current.isStopped() || !mc.getSoundManager().isActive(current))
        ) {
            current = new ContaminationLoopSound();
            mc.getSoundManager().play(current);
        }
    }

    private static void updateGate(RandomSource random) {
        if (activity >= 0.99F) {
            gateOpen = true;
            gateTimer = 0;
            return;
        }
        if (--gateTimer > 0) {
            return;
        }
        gateOpen = !gateOpen;
        var swell = 80 + random.nextInt(80 + 1);
        gateTimer = gateOpen ? swell : Math.round(swell * (1.0F / Math.max(0.05F, activity) - 1.0F));
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
