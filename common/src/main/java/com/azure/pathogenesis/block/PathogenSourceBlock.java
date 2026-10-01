package com.azure.pathogenesis.block;

import com.azure.pathogenesis.blockentity.PathogenSourceBlockEntity;
import com.azure.pathogenesis.contamination.ContainmentState;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.contamination.RuptureStrength;
import com.azure.pathogenesis.registry.PathogenBlockEntities;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * The Pathogen Source: a containment canister. Nothing happens until a player, projectile or explosion breaches it,
 * every zone traces back to one of these events.
 * <ul>
 * <li>Hit (attack / projectile): SEALED → DAMAGED → LEAKING → OPEN.</li>
 * <li>DAMAGED left alone starts leaking by itself after a short delay (block entity timer).</li>
 * <li>Broken by hand: ruptures. Silk Touch on a SEALED source safely recovers a Sealed Pathogen Ampule.</li>
 * <li>Explosion: strongest rupture.</li>
 * </ul>
 */
public class PathogenSourceBlock extends BaseEntityBlock {

    public static final MapCodec<PathogenSourceBlock> CODEC = simpleCodec(PathogenSourceBlock::new);

    public static final EnumProperty<ContainmentState> CONTAINMENT = EnumProperty.create(
        "containment",
        ContainmentState.class
    );

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 14.0D, 13.0D);

    public PathogenSourceBlock(Properties properties) {
        super(properties);
        registerDefaultState(
            stateDefinition.any().setValue(CONTAINMENT, ContainmentState.SEALED).setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTAINMENT, FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new PathogenSourceBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide() || !state.getValue(CONTAINMENT).needsTicking()) {
            return null;
        }
        return createTickerHelper(
            type,
            PathogenBlockEntities.PATHOGEN_SOURCE.get(),
            PathogenSourceBlockEntity::serverTick
        );
    }

    @Override
    protected void attack(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player
    ) {
        if (level instanceof ServerLevel serverLevel && !player.isCreative()) {
            advance(serverLevel, pos, state);
        }
        super.attack(state, level, pos, player);
    }

    @Override
    protected void onProjectileHit(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockHitResult hit,
        @NotNull Projectile projectile
    ) {
        if (level.isClientSide() || projectile.getType().is(PathogenTags.Entities.SOURCE_PROJECTILE_IMMUNE)) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            advance(serverLevel, hit.getBlockPos(), state);
        }
    }

    public static void advance(ServerLevel level, BlockPos pos, BlockState state) {
        var current = state.getValue(CONTAINMENT);
        var next = switch (current) {
            case SEALED -> ContainmentState.DAMAGED;
            case DAMAGED -> ContainmentState.LEAKING;
            case LEAKING -> ContainmentState.OPEN;
            case OPEN, EMPTY -> current;
        };
        if (next == current) {
            return;
        }
        setContainment(level, pos, state, next);
        if (next == ContainmentState.DAMAGED) {
            level.playSound(null, pos, PathogenSounds.CANISTER_CRACK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (level.getBlockEntity(pos) instanceof PathogenSourceBlockEntity source) {
            source.beginLeak(level, next == ContainmentState.OPEN ? RuptureStrength.RUPTURE : RuptureStrength.CRACK);
        }
    }

    public static void setContainment(Level level, BlockPos pos, BlockState state, ContainmentState next) {
        level.setBlock(pos, state.setValue(CONTAINMENT, next), Block.UPDATE_ALL);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        if (level instanceof ServerLevel serverLevel && !player.isCreative()) {
            var containment = state.getValue(CONTAINMENT);
            var safeRecovery = containment == ContainmentState.SEALED && hasSilkTouch(
                serverLevel,
                player.getMainHandItem()
            );
            if (!safeRecovery && containment != ContainmentState.EMPTY) {
                PathogenZoneManager.onRupture(serverLevel, pos, RuptureStrength.RUPTURE, false);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onExplosionHit(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Explosion explosion,
        @NotNull BiConsumer<ItemStack, BlockPos> dropConsumer
    ) {
        if (level instanceof ServerLevel serverLevel && state.getValue(CONTAINMENT) != ContainmentState.EMPTY) {
            PathogenZoneManager.onRupture(serverLevel, pos, RuptureStrength.EXPLOSION, false);
        }
        super.onExplosionHit(state, level, pos, explosion, dropConsumer);
    }

    @Override
    protected void onRemove(
        BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        BlockState newState,
        boolean movedByPiston
    ) {
        if (
            !state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof PathogenSourceBlockEntity source
        ) {
            PathogenZoneManager.onSourceInactive(serverLevel, source.zoneId());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static boolean hasSilkTouch(ServerLevel level, ItemStack stack) {
        var silkTouch = level.registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getHolderOrThrow(Enchantments.SILK_TOUCH);
        return EnchantmentHelper.getItemEnchantmentLevel(silkTouch, stack) > 0;
    }

    @Override
    public void animateTick(
        BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        var containment = state.getValue(CONTAINMENT);
        if (!containment.isLeaking() && containment != ContainmentState.DAMAGED) {
            return;
        }
        var count = containment == ContainmentState.OPEN ? 4 : containment == ContainmentState.LEAKING ? 2 : 1;
        for (var i = 0; i < count; i++) {
            if (containment == ContainmentState.DAMAGED && random.nextInt(4) != 0) {
                continue;
            }
            level.addParticle(
                PathogenZoneManager.PATHOGEN_DUST,
                pos.getX() + 0.3D + random.nextDouble() * 0.4D,
                pos.getY() + 0.8D + random.nextDouble() * 0.3D,
                pos.getZ() + 0.3D + random.nextDouble() * 0.4D,
                (random.nextDouble() - 0.5D) * 0.02D,
                0.02D + random.nextDouble() * 0.02D,
                (random.nextDouble() - 0.5D) * 0.02D
            );
        }
    }
}
