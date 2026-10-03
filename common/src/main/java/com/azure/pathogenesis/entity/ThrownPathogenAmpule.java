package com.azure.pathogenesis.entity;

import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.contamination.RuptureStrength;
import com.azure.pathogenesis.registry.PathogenEntities;
import com.azure.pathogenesis.registry.PathogenItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class ThrownPathogenAmpule extends ThrowableItemProjectile {

    public ThrownPathogenAmpule(EntityType<? extends ThrownPathogenAmpule> type, Level level) {
        super(type, level);
    }

    public ThrownPathogenAmpule(Level level, LivingEntity shooter) {
        super(PathogenEntities.THROWN_PATHOGEN_AMPULE.get(), shooter, level);
    }

    public ThrownPathogenAmpule(Level level, double x, double y, double z) {
        super(PathogenEntities.THROWN_PATHOGEN_AMPULE.get(), x, y, z, level);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return PathogenItems.SEALED_PATHOGEN_AMPULE.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05D;
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        var pos = result instanceof BlockHitResult blockHit
            ? blockHit.getBlockPos().relative(blockHit.getDirection())
            : BlockPos.containing(result.getLocation());
        level.levelEvent(LevelEvent.PARTICLES_SPELL_POTION_SPLASH, blockPosition(), 0x141218);
        PathogenZoneManager.onRupture(level, pos, RuptureStrength.RUPTURE, false);
        discard();
    }
}
