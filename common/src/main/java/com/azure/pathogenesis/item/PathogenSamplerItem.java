package com.azure.pathogenesis.item;

import com.azure.pathogenesis.Pathogenesis;
import com.azure.pathogenesis.block.PathogenSourceBlock;
import com.azure.pathogenesis.block.SporePlantBlock;
import com.azure.pathogenesis.blockentity.PathogenSourceBlockEntity;
import com.azure.pathogenesis.contamination.ContainmentState;
import com.azure.pathogenesis.contamination.PathogenSavedData;
import com.azure.pathogenesis.contamination.PathogenZone;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.exposure.ExposureType;
import com.azure.pathogenesis.exposure.PathogenExposureHelper;
import com.azure.pathogenesis.item.sampler.SampleResult;
import com.azure.pathogenesis.registry.PathogenBlocks;
import com.azure.pathogenesis.registry.PathogenDataComponents;
import com.azure.pathogenesis.registry.PathogenTags;
import com.azure.pathogenesis.registry.PathogenTriggers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PathogenSamplerItem extends Item {

    private static final String[] COMPASS = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };

    private static final Component SEPARATOR = Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY);

    private enum Target {
        GROUND,
        WATER,
        PLANT,
        SOURCE
    }

    public PathogenSamplerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return sample(context.getLevel(), player, context.getHand());
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level,
        @NotNull Player player,
        @NotNull InteractionHand hand
    ) {
        var stack = player.getItemInHand(hand);
        var result = sample(level, player, hand);
        return result.consumesAction()
            ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide())
            : InteractionResultHolder.pass(stack);
    }

    private InteractionResult sample(Level level, Player player, InteractionHand hand) {
        var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        var stack = player.getItemInHand(hand);
        var config = Pathogenesis.getConfig().samplerConfigs;
        var detailed = player.isSecondaryUseActive();
        var pos = hit.getBlockPos();
        var state = serverLevel.getBlockState(pos);
        var target = classify(serverLevel, pos, state);

        var reading = read(serverLevel, pos, state, target, detailed);
        stack.set(PathogenDataComponents.SAMPLE_RESULT.get(), reading.result);
        player.displayClientMessage(reading.message, !detailed);

        feedback(serverLevel, player, hit, reading);
        applyRisk(serverLevel, player, pos, state, target, reading.contaminated);

        if (reading.result != SampleResult.CLEAN && player instanceof ServerPlayer serverPlayer) {
            PathogenTriggers.trigger(serverPlayer, PathogenTriggers.FIELD_SAMPLE);
        }
        player.getCooldowns().addCooldown(this, config.samplerCooldown);
        var cost = 1 + (detailed ? 1 : 0) + (target == Target.SOURCE ? 2 : 0);
        stack.hurtAndBreak(cost, player, LivingEntity.getSlotForHand(hand));
        return InteractionResult.CONSUME;
    }

    private static Target classify(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof PathogenSourceBlock) {
            return Target.SOURCE;
        }
        if (!level.getFluidState(pos).isEmpty()) {
            return Target.WATER;
        }
        if (state.getBlock() instanceof BushBlock || state.is(PathogenBlocks.CONTAMINATED_ROOTS.get())) {
            return Target.PLANT;
        }
        return Target.GROUND;
    }

    private record Reading(
        SampleResult result,
        boolean contaminated,
        MutableComponent message
    ) {}

    private static Reading read(ServerLevel level, BlockPos pos, BlockState state, Target target, boolean detailed) {
        var contaminated = state.is(PathogenTags.Blocks.CONTAMINATED);
        var zone = target == Target.SOURCE ? sourceZone(level, pos) : PathogenZoneManager.findZone(level, pos);
        if (zone != null && target != Target.SOURCE && !contaminated && !zone.isWithinRadius(pos)) {
            zone = null;
        }

        var result = zone != null ? SampleResult.forStage(zone.stage()) : SampleResult.CLEAN;
        if (contaminated) {
            result = result.worst(SampleResult.TRACE);
        }
        if (target == Target.SOURCE && zone == null) {
            var containment = state.getValue(PathogenSourceBlock.CONTAINMENT);
            if (containment == ContainmentState.DAMAGED) {
                result = SampleResult.TRACE;
            }
        }

        var message = Component.empty()
            .append(
                Component.translatable("sampler.pathogenesis.result." + result.id())
                    .withStyle(result.style, ChatFormatting.BOLD)
            )
            .append(SEPARATOR)
            .append(state.getBlock().getName().withStyle(ChatFormatting.WHITE));

        switch (target) {
            case SOURCE -> appendSource(level, pos, state, message);
            case PLANT -> appendPlant(state, message);
            default -> {}
        }

        if (zone != null) {
            appendZone(level, zone, message);
            if (detailed) {
                append(message, origin(pos, zone));
            }
        } else if (contaminated) {
            append(message, Component.translatable("sampler.pathogenesis.isolated").withStyle(ChatFormatting.GRAY));
        } else if (target != Target.SOURCE) {
            append(message, Component.translatable("sampler.pathogenesis.none").withStyle(ChatFormatting.GRAY));
        }
        return new Reading(result, contaminated, message);
    }

    @Nullable
    private static PathogenZone sourceZone(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PathogenSourceBlockEntity source) {
            return PathogenSavedData.get(level).get(source.zoneId());
        }
        return null;
    }

    private static void appendSource(ServerLevel level, BlockPos pos, BlockState state, MutableComponent message) {
        var containment = state.getValue(PathogenSourceBlock.CONTAINMENT);
        var style = switch (containment) {
            case SEALED -> ChatFormatting.GREEN;
            case DAMAGED -> ChatFormatting.YELLOW;
            case LEAKING, OPEN -> ChatFormatting.RED;
            case EMPTY -> ChatFormatting.GRAY;
        };
        append(
            message,
            Component.translatable("sampler.pathogenesis.containment." + containment.getSerializedName())
                .withStyle(style)
        );
        if (
            containment != ContainmentState.EMPTY
                && level.getBlockEntity(pos) instanceof PathogenSourceBlockEntity source
        ) {
            var percent = Mth.ceil(100.0F * source.pathogen() / PathogenSourceBlockEntity.CAPACITY);
            append(message, Component.translatable("sampler.pathogenesis.remaining", percent));
        }
        if (PathogenSourceBlock.isStabilized(state)) {
            append(
                message,
                Component.translatable("sampler.pathogenesis.stabilized").withStyle(ChatFormatting.AQUA)
            );
        }
    }

    private static void appendPlant(BlockState state, MutableComponent message) {
        if (!(state.getBlock() instanceof SporePlantBlock)) {
            return;
        }
        String pod;
        ChatFormatting style;
        if (state.getValue(SporePlantBlock.AGE) < SporePlantBlock.MAX_AGE) {
            pod = "immature";
            style = ChatFormatting.GRAY;
        } else if (state.getValue(SporePlantBlock.TRIGGERED)) {
            pod = "rattling";
            style = ChatFormatting.DARK_RED;
        } else if (SporePlantBlock.isPrimed(state)) {
            pod = "primed";
            style = ChatFormatting.RED;
        } else {
            pod = "recharging";
            style = ChatFormatting.YELLOW;
        }
        append(message, Component.translatable("sampler.pathogenesis.pod." + pod).withStyle(style));
    }

    private static void appendZone(ServerLevel level, PathogenZone zone, MutableComponent message) {
        append(
            message,
            zone.isSourceActive()
                ? Component.translatable("sampler.pathogenesis.source_fed").withStyle(ChatFormatting.RED)
                : Component.translatable("sampler.pathogenesis.source_spent").withStyle(ChatFormatting.GRAY)
        );
        if (zone.isThawing(level.getGameTime())) {
            append(
                message,
                Component.translatable("sampler.pathogenesis.thawing")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
            );
        } else if (zone.isChilled()) {
            append(message, Component.translatable("sampler.pathogenesis.dormant").withStyle(ChatFormatting.AQUA));
        }
    }

    private static Component origin(BlockPos pos, PathogenZone zone) {
        var dx = zone.origin().getX() - pos.getX();
        var dz = zone.origin().getZ() - pos.getZ();
        var distance = Mth.floor(Math.sqrt(dx * dx + dz * dz));
        if (distance < 3) {
            return Component.translatable("sampler.pathogenesis.origin_here").withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        var bearing = Math.toDegrees(Math.atan2(dx, -dz));
        var index = Math.floorMod((int) Math.round(bearing / 45.0D), COMPASS.length);
        return Component.translatable("sampler.pathogenesis.origin", distance, COMPASS[index])
            .withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    private static void append(MutableComponent message, Component part) {
        message.append(SEPARATOR).append(part);
    }

    private static void feedback(ServerLevel level, Player player, BlockHitResult hit, Reading reading) {
        var at = hit.getLocation();
        level.playSound(null, hit.getBlockPos(), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.6F, 1.2F);
        level.playSound(
            null,
            player.blockPosition(),
            SoundEvents.NOTE_BLOCK_BIT.value(),
            SoundSource.PLAYERS,
            0.5F,
            reading.result.pitch
        );
        if (reading.contaminated) {
            level.sendParticles(PathogenZoneManager.PATHOGEN_DUST, at.x, at.y, at.z, 6, 0.15D, 0.1D, 0.15D, 0.01D);
        }
    }

    private static void applyRisk(
        ServerLevel level,
        Player player,
        BlockPos pos,
        BlockState state,
        Target target,
        boolean contaminated
    ) {
        var config = Pathogenesis.getConfig().samplerConfigs;
        if (target == Target.SOURCE) {
            if (state.getValue(PathogenSourceBlock.CONTAINMENT).isLeaking()) {
                expose(player, config.samplerSourceExposure, ExposureType.DIRECT);
            }
        } else if (contaminated) {
            expose(player, config.samplerContactExposure, ExposureType.CONTACT);
        }
        if (
            target == Target.PLANT && SporePlantBlock.isPrimed(state)
                && level.random.nextDouble() < config.samplerPodTriggerChance
        ) {
            SporePlantBlock.trigger(level, pos, state);
        }
    }

    private static void expose(Player player, int amount, ExposureType type) {
        if (amount <= 0) {
            return;
        }
        var helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.isEmpty() && helmet.is(PathogenTags.Items.SPORE_FILTERS)) {
            helmet.hurtAndBreak(1, player, EquipmentSlot.HEAD);
            return;
        }
        PathogenExposureHelper.expose(player, amount, type);
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack stack, @NotNull ItemStack repair) {
        return repair.is(Items.COPPER_INGOT);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        var last = stack.get(PathogenDataComponents.SAMPLE_RESULT.get());
        if (last != null) {
            tooltip.add(
                Component.translatable(
                    "item.pathogenesis.pathogen_sampler.last",
                    Component.translatable("sampler.pathogenesis.result." + last.id()).withStyle(last.style)
                ).withStyle(ChatFormatting.GRAY)
            );
        }
        tooltip.add(
            Component.translatable("item.pathogenesis.pathogen_sampler.tooltip").withStyle(ChatFormatting.DARK_GRAY)
        );
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
