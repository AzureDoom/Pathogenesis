package com.azure.pathogenesis.block;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.blockentity.SporePlantBlockEntity;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.entity.SporeCloudEntity;
import com.azure.pathogenesis.registry.PathogenSounds;
import com.azure.pathogenesis.registry.PathogenTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

public class SporePlantBlock extends BushBlock implements EntityBlock {

    public static final MapCodec<SporePlantBlock> CODEC = simpleCodec(SporePlantBlock::new);

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    public static final BooleanProperty READY = BooleanProperty.create("ready");

    public static final BooleanProperty TRIGGERED = BooleanProperty.create("triggered");

    public static final int MAX_AGE = 3;

    private static final VoxelShape[] SHAPES = {
        Block.box(5.0D, 0.0D, 5.0D, 11.0D, 5.0D, 11.0D),
        Block.box(4.0D, 0.0D, 4.0D, 12.0D, 9.0D, 12.0D),
        Block.box(3.0D, 0.0D, 3.0D, 13.0D, 12.0D, 13.0D),
        Block.box(2.0D, 0.0D, 2.0D, 14.0D, 15.0D, 14.0D)
    };

    public SporePlantBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(READY, false).setValue(TRIGGERED, false));
    }

    @Override
    protected @NotNull MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new SporePlantBlockEntity(pos, state);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(BlockState state) {
        return state.getValue(AGE) == MAX_AGE ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, READY, TRIGGERED);
    }

    public static boolean isPrimed(BlockState state) {
        return state.getValue(AGE) == MAX_AGE && state.getValue(READY) && !state.getValue(TRIGGERED);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return state.is(PathogenTags.Blocks.CONTAMINATED_SOIL);
    }

    @Override
    protected @NotNull VoxelShape getShape(
        BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        if (state.getValue(AGE) == MAX_AGE) {
            return SporePodLayout.shape(pos);
        }
        var offset = state.getOffset(level, pos);
        return SHAPES[state.getValue(AGE)].move(offset.x, offset.y, offset.z);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE || !state.getValue(READY) && !state.getValue(TRIGGERED);
    }

    @Override
    protected void randomTick(
        @NotNull BlockState state,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        var rate = Pathogenesis.getConfig().floraConfigs.pathogenPlantGrowthRate;
        if (rate <= 0.0D || random.nextDouble() > Math.min(1.0D, 0.25D * rate)) {
            return;
        }
        var age = (int) state.getValue(AGE);
        if (age < MAX_AGE) {
            var grown = state.setValue(AGE, age + 1);
            level.setBlock(pos, age + 1 == MAX_AGE ? grown.setValue(READY, true) : grown, Block.UPDATE_ALL);
        } else if (
            !state.getValue(READY) && !state.getValue(TRIGGERED) && !level.getBlockTicks().hasScheduledTick(pos, this)
        ) {
            level.setBlock(pos, state.setValue(READY, true), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void entityInside(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Entity entity
    ) {
        if (!(level instanceof ServerLevel serverLevel) || !isPrimed(state)) {
            return;
        }
        if (entity instanceof LivingEntity living) {
            if (living.getType().is(PathogenTags.Entities.SPORE_IMMUNE)) {
                return;
            }
            if (entity.isSteppingCarefully() && living.getRandom().nextInt(4) != 0) {
                return;
            }
        } else if (entity instanceof ItemEntity) {
            return;
        }
        trigger(serverLevel, pos, state);
    }

    @Override
    protected void attack(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player
    ) {
        if (level instanceof ServerLevel serverLevel && isPrimed(state)) {
            trigger(serverLevel, pos, state);
        }
        super.attack(state, level, pos, player);
    }

    public static boolean trigger(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof SporePlantBlock plant) || !isPrimed(state)) {
            return false;
        }
        level.setBlock(pos, state.setValue(TRIGGERED, true), Block.UPDATE_ALL);
        level.playSound(
            null,
            pos,
            PathogenSounds.SPORE_PLANT_RATTLE.get(),
            SoundSource.BLOCKS,
            0.9F,
            0.9F + level.random.nextFloat() * 0.2F
        );
        level.scheduleTick(
            pos,
            plant,
            10 + level.random.nextInt(16 - 10 + 1)
        );
        return true;
    }

    public static void disturbNearby(ServerLevel level, BlockPos center, int radius) {
        for (var pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof SporePlantBlock && isPrimed(state)) {
                trigger(level, pos.immutable(), state);
            }
        }
    }

    @Override
    protected void tick(
        BlockState state,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        if (state.getValue(TRIGGERED)) {
            release(level, pos);
            if (level.getBlockEntity(pos) instanceof SporePlantBlockEntity plant) {
                plant.playSpray();
            }
            level.blockEvent(pos, this, 1, 0);
            level.setBlock(pos, state.setValue(TRIGGERED, false).setValue(READY, false), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, 1200 + random.nextInt(3600 - 1200 + 1));
        } else if (state.getValue(AGE) == MAX_AGE && !state.getValue(READY)) {
            level.setBlock(pos, state.setValue(READY, true), Block.UPDATE_ALL);
        }
    }

    public static void release(ServerLevel level, BlockPos pos) {
        SporeCloudEntity.spawn(
            level,
            Vec3.atBottomCenterOf(pos).add(0.0D, 0.4D, 0.0D),
            PathogenZoneManager.findZoneId(level, pos)
        );
        level.playSound(null, pos, PathogenSounds.SPORE_RELEASE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        if (level instanceof ServerLevel serverLevel && !player.isCreative() && isPrimed(state)) {
            release(serverLevel, pos);
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
        if (level instanceof ServerLevel serverLevel && state.getValue(AGE) == MAX_AGE) {
            release(serverLevel, pos);
        }
        super.onExplosionHit(state, level, pos, explosion, dropConsumer);
    }

    @Override
    protected boolean triggerEvent(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        int id,
        int param
    ) {
        if (id != 1) {
            return super.triggerEvent(state, level, pos, id, param);
        }
        if (level.isClientSide()) {
            var random = level.getRandom();
            for (var emitter : emitters(level, pos)) {
                for (var i = 0; i < 8; i++) {
                    level.addParticle(
                        SporeCloudEntity.SPORE_DUST,
                        emitter.x + (random.nextDouble() - 0.5D) * 0.08D,
                        emitter.y,
                        emitter.z + (random.nextDouble() - 0.5D) * 0.08D,
                        (random.nextDouble() - 0.5D) * 0.12D,
                        0.08D + random.nextDouble() * 0.08D,
                        (random.nextDouble() - 0.5D) * 0.12D
                    );
                }
            }
        }
        return true;
    }

    @Override
    public void animateTick(
        BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        if (state.getValue(TRIGGERED)) {
            for (var emitter : emitters(level, pos)) {
                level.addParticle(
                    SporeCloudEntity.SPORE_DUST,
                    emitter.x + (random.nextDouble() - 0.5D) * 0.1D,
                    emitter.y,
                    emitter.z + (random.nextDouble() - 0.5D) * 0.1D,
                    0.0D,
                    0.03D,
                    0.0D
                );
            }
        } else if (isPrimed(state) && random.nextInt(10) == 0) {
            var emitters = emitters(level, pos);
            var emitter = emitters.get(random.nextInt(emitters.size()));
            level.addParticle(SporeCloudEntity.SPORE_DUST, emitter.x, emitter.y, emitter.z, 0.0D, 0.005D, 0.0D);
        }
    }

    private static List<Vec3> emitters(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SporePlantBlockEntity plant) {
            var points = plant.emitterPositions();
            if (!points.isEmpty()) {
                return points;
            }
        }
        return List.of(new Vec3(pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D));
    }

    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 100;
    }
}
