package io.iridium.overvaults.network;

import io.iridium.overvaults.millenium.gui.OvervaultGuiUpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundOvervaultGuiRequestPacket {
    public ServerboundOvervaultGuiRequestPacket() {
    }

    public static void encode(ServerboundOvervaultGuiRequestPacket packet, FriendlyByteBuf buf) {
    }

    public static ServerboundOvervaultGuiRequestPacket decode(FriendlyByteBuf buf) {
        return new ServerboundOvervaultGuiRequestPacket();
    }

    public static void handle(ServerboundOvervaultGuiRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) {
                OvervaultGuiUpdateManager.open(player);
            }
        });
        context.setPacketHandled(true);
    }
}
