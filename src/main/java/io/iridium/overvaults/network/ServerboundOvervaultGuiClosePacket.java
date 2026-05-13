package io.iridium.overvaults.network;

import io.iridium.overvaults.millenium.gui.OvervaultGuiUpdateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundOvervaultGuiClosePacket {
    public ServerboundOvervaultGuiClosePacket() {
    }

    public static void encode(ServerboundOvervaultGuiClosePacket packet, FriendlyByteBuf buf) {
    }

    public static ServerboundOvervaultGuiClosePacket decode(FriendlyByteBuf buf) {
        return new ServerboundOvervaultGuiClosePacket();
    }

    public static void handle(ServerboundOvervaultGuiClosePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) {
                OvervaultGuiUpdateManager.close(player.getUUID());
            }
        });
        context.setPacketHandled(true);
    }
}
