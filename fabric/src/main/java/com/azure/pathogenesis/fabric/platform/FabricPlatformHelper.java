package com.azure.pathogenesis.fabric.platform;

import com.azure.pathogenesis.fluid.ContaminatedWaterFluid;
import com.azure.pathogenesis.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public ContaminatedWaterFluid.Source createContaminatedWaterSource() {
        return new ContaminatedWaterFluid.Source();
    }

    @Override
    public ContaminatedWaterFluid.Flowing createContaminatedWaterFlowing() {
        return new ContaminatedWaterFluid.Flowing();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
