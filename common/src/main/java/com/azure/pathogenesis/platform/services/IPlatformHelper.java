package com.azure.pathogenesis.platform.services;

import com.azure.pathogenesis.fluid.ContaminatedWaterFluid;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public interface IPlatformHelper {

    boolean isModLoaded(String modId);

    ContaminatedWaterFluid.Source createContaminatedWaterSource();

    ContaminatedWaterFluid.Flowing createContaminatedWaterFlowing();

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
