package com.azure.pathogenesis.command;

import com.azure.pathogenesis.contamination.PathogenSterilization;
import com.azure.pathogenesis.contamination.PathogenZoneManager;
import com.azure.pathogenesis.contamination.RuptureStrength;
import com.azure.pathogenesis.infection.InfectionSite;
import com.azure.pathogenesis.infection.NeomorphInfections;
import com.azure.pathogenesis.infection.PathogenHosts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;

public final class PathogenCommands {

    private PathogenCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("pathogenesis")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("zones").executes(ctx -> listZones(ctx.getSource())))
                .then(
                    Commands.literal("rupture")
                        .executes(ctx -> rupture(ctx.getSource(), BlockPos.containing(ctx.getSource().getPosition())))
                        .then(
                            Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(
                                    ctx -> rupture(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"))
                                )
                        )
                )
                .then(
                    Commands.literal("sterilize")
                        .then(
                            Commands.argument("radius", IntegerArgumentType.integer(1, 32))
                                .executes(
                                    ctx -> sterilize(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius"))
                                )
                        )
                )
                .then(
                    Commands.literal("infect")
                        .then(
                            Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> infect(ctx.getSource(), EntityArgument.getEntities(ctx, "targets")))
                        )
                )
                .then(
                    Commands.literal("cure")
                        .then(
                            Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> cure(ctx.getSource(), EntityArgument.getEntities(ctx, "targets")))
                        )
                )
                .then(
                    Commands.literal("status")
                        .then(
                            Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> status(ctx.getSource(), EntityArgument.getEntity(ctx, "target")))
                        )
                )
        );
    }

    private static int listZones(CommandSourceStack source) {
        var zones = PathogenZoneManager.zones(source.getLevel());
        source.sendSuccess(() -> Component.translatable("commands.pathogenesis.zones.header", zones.size()), false);
        for (var zone : zones) {
            var o = zone.origin();
            source.sendSuccess(
                () -> Component.translatable(
                    "commands.pathogenesis.zones.entry",
                    o.getX(),
                    o.getY(),
                    o.getZ(),
                    zone.stage().id(),
                    zone.radius(),
                    zone.contamination(),
                    zone.isSourceActive() ? "yes" : "no"
                ),
                false
            );
        }
        return zones.size();
    }

    private static int rupture(CommandSourceStack source, BlockPos pos) {
        PathogenZoneManager.onRupture(source.getLevel(), pos, RuptureStrength.RUPTURE, false);
        source.sendSuccess(
            () -> Component.translatable("commands.pathogenesis.rupture", pos.getX(), pos.getY(), pos.getZ()),
            true
        );
        return 1;
    }

    private static int sterilize(CommandSourceStack source, int radius) {
        var count = PathogenSterilization.sterilizeArea(
            source.getLevel(),
            BlockPos.containing(source.getPosition()),
            radius
        );
        source.sendSuccess(() -> Component.translatable("commands.pathogenesis.sterilize", count), true);
        return count;
    }

    private static int infect(CommandSourceStack source, Collection<? extends Entity> targets) {
        var level = source.getLevel();
        var count = 0;
        for (var entity : targets) {
            if (
                entity instanceof LivingEntity living && NeomorphInfections.isValidHost(living)
                    && NeomorphInfections.infect(
                        living,
                        new InfectionSite(
                            living.blockPosition(),
                            PathogenZoneManager.findZoneId(level, living.blockPosition())
                        )
                    )
            ) {
                count++;
            }
        }
        var infected = count;
        source.sendSuccess(() -> Component.translatable("commands.pathogenesis.infect", infected), true);
        return infected;
    }

    private static int cure(CommandSourceStack source, Collection<? extends Entity> targets) {
        var count = 0;
        for (var entity : targets) {
            if (entity instanceof LivingEntity living) {
                if (PathogenHosts.get(living) != null) {
                    PathogenHosts.remove(living);
                    count++;
                }
            }
        }
        var cured = count;
        source.sendSuccess(() -> Component.translatable("commands.pathogenesis.cure", cured), true);
        return cured;
    }

    private static int status(CommandSourceStack source, Entity entity) throws CommandSyntaxException {
        if (!(entity instanceof LivingEntity living)) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
        }
        var host = PathogenHosts.get(living);
        var exposure = host == null ? null : host.exposure();
        var infection = host == null ? null : host.infection();
        source.sendSuccess(
            () -> Component.translatable(
                "commands.pathogenesis.status",
                living.getDisplayName(),
                exposure == null ? 0 : exposure.exposure(),
                infection == null ? "none" : infection.stage().name().toLowerCase(java.util.Locale.ROOT),
                infection == null ? 0 : (int) (infection.progress() * 100)
            ),
            false
        );
        return 1;
    }
}
