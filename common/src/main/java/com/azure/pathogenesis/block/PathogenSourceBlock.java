package com.azure.pathogenesis.block;

import com.azure.pathogenesis.blockentity.PathogenSourceBlockEntity;
import com.azure.pathogenesis.contamination.ContainmentState;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.contamination.RuptureStrength;
import com.azure.pathogenesis.item.PathogenSamplerItem;
import com.azure.pathogenesis.registry.PathogenBlockEntities;
import com.azure.pathogenesis.registry.PathogenItems;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class PathogenSourceBlock extends BaseEntityBlock {

    public static final MapCodec<PathogenSourceBlock> CODEC = simpleCodec(PathogenSourceBlock::new);

    public static final EnumProperty<ContainmentState> CONTAINMENT = EnumProperty.create(
        "containment",
        ContainmentState.class
    );

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public static final BooleanProperty PACKED = BooleanProperty.create("packed");

    public static final BooleanProperty FROZEN = BooleanProperty.create("frozen");

    public static final int AMPULE_COUNT = 4;

    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 14.0D, 13.0D);

    public PathogenSourceBlock(Properties properties) {
        super(properties);
        registerDefaultState(
            stateDefinition.any()
                .setValue(CONTAINMENT, ContainmentState.SEALED)
                .setValue(FACING, Direction.NORTH)
                .setValue(PACKED, false)
                .setValue(FROZEN, false)
        );
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTAINMENT, FACING, PACKED, FROZEN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return withFrozen(
            defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()),
            context.getLevel(),
            context.getClickedPos()
        );
    }

    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighborState,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighborPos
    ) {
        return direction.getAxis().isHorizontal() ? withFrozen(state, level, pos) : state;
    }

    private static BlockState withFrozen(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(FROZEN, state.getValue(PACKED) || isIcedIn(level, pos));
    }

    private static boolean isIcedIn(BlockGetter level, BlockPos pos) {
        for (var direction : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(pos.relative(direction)).is(Blocks.BLUE_ICE)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isStabilized(BlockState state) {
        var containment = state.getValue(CONTAINMENT);
        return state.getValue(FROZEN) && (containment == ContainmentState.SEALED
            || containment == ContainmentState.DAMAGED);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
        @NotNull ItemStack stack,
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hit
    ) {
        if (stack.getItem() instanceof PathogenSamplerItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        var containment = state.getValue(CONTAINMENT);
        var intact = containment == ContainmentState.SEALED || containment == ContainmentState.DAMAGED;
        if (stack.is(Items.POWDER_SNOW_BUCKET) && intact && !state.getValue(PACKED)) {
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.setBlock(pos, state.setValue(PACKED, true).setValue(FROZEN, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_POWDER_SNOW, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (stack.is(Items.BUCKET) && state.getValue(PACKED)) {
            if (!level.isClientSide()) {
                player.setItemInHand(
                    hand,
                    ItemUtils.createFilledResult(stack, player, new ItemStack(Items.POWDER_SNOW_BUCKET))
                );
                level.setBlock(pos, withFrozen(state.setValue(PACKED, false), level, pos), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL_POWDER_SNOW, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        var containment = state.getValue(CONTAINMENT);
        if (containment != ContainmentState.SEALED && containment != ContainmentState.DAMAGED) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (!isStabilized(state)) {
            var source = serverLevel.getBlockEntity(pos) instanceof PathogenSourceBlockEntity be ? be : null;
            if (source != null && source.warnOpenAttempt()) {
                player.displayClientMessage(
                    Component.translatable("block.pathogenesis.pathogen_source.unstable").withStyle(ChatFormatting.RED),
                    true
                );
                if (state.getValue(CONTAINMENT) == ContainmentState.SEALED) {
                    advance(serverLevel, pos, state);
                } else {
                    serverLevel.playSound(
                        null,
                        pos,
                        PathogenSounds.PATHOGEN_LEAK.get(),
                        SoundSource.BLOCKS,
                        0.6F,
                        1.5F
                    );
                }
                return InteractionResult.SUCCESS;
            }
            forceOpen(serverLevel, pos, state, source, player);
            return InteractionResult.SUCCESS;
        }
        releaseAmpules(serverLevel, pos);
        setContainment(serverLevel, pos, state, ContainmentState.EMPTY);
        serverLevel.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.6F, 1.6F);
        serverLevel.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8F, 0.7F);
        player.displayClientMessage(
            Component.translatable("block.pathogenesis.pathogen_source.opened").withStyle(ChatFormatting.AQUA),
            true
        );
        return InteractionResult.SUCCESS;
    }

    private static void forceOpen(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        @Nullable PathogenSourceBlockEntity source,
        Player player
    ) {
        setContainment(level, pos, state, ContainmentState.OPEN);
        if (source != null) {
            source.beginLeak(level, RuptureStrength.RUPTURE);
        } else {
            PathogenZoneManager.onRupture(level, pos, RuptureStrength.RUPTURE, false);
        }
        player.displayClientMessage(
            Component.translatable("block.pathogenesis.pathogen_source.forced").withStyle(ChatFormatting.DARK_RED),
            true
        );
    }

    private static void releaseAmpules(ServerLevel level, BlockPos pos) {
        for (var i = 0; i < AMPULE_COUNT; i++) {
            popResourceFromFace(level, pos, Direction.UP, new ItemStack(PathogenItems.SEALED_PATHOGEN_AMPULE.get()));
        }
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
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    public static ContainmentState itemDisplayState(ItemStack stack) {
        var stored = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(CONTAINMENT);
        if (stored == null) {
            return ContainmentState.SEALED;
        }
        return switch (stored) {
            case SEALED, DAMAGED, LEAKING -> stored;
            case OPEN -> ContainmentState.LEAKING;
            case EMPTY -> ContainmentState.SEALED;
        };
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
            if (isStabilized(state)) {
                serverLevel.playSound(null, pos, SoundEvents.GLASS_HIT, SoundSource.BLOCKS, 0.8F, 0.6F);
            } else {
                advance(serverLevel, pos, state);
            }
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
        if (
            level.isClientSide() || projectile.getType().is(PathogenTags.Entities.SOURCE_PROJECTILE_IMMUNE)
                || isStabilized(state)
        ) {
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
            var safeRecovery = isStabilized(state) || containment == ContainmentState.SEALED && hasSilkTouch(
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
        if (state.getValue(FROZEN) && random.nextInt(3) == 0) {
            level.addParticle(
                ParticleTypes.SNOWFLAKE,
                pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                pos.getY() + 0.2D + random.nextDouble() * 0.8D,
                pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                0.0D,
                -0.01D,
                0.0D
            );
        }
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
