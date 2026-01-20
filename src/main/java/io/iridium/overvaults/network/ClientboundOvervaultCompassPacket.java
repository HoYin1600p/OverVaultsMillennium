package io.iridium.overvaults.network;

import io.iridium.overvaults.client.OvervaultCompassHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundOvervaultCompassPacket {
    private final boolean hasTarget;
    private final BlockPos target;

    public ClientboundOvervaultCompassPacket(BlockPos target) {
        this.hasTarget = target != null;
        this.target = target;
    }

    public static ClientboundOvervaultCompassPacket clear() {
        return new ClientboundOvervaultCompassPacket(null);
    }

    public static void encode(ClientboundOvervaultCompassPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.hasTarget);
        if (packet.hasTarget) {
            buf.writeBlockPos(packet.target);
        }
    }

    public static ClientboundOvervaultCompassPacket decode(FriendlyByteBuf buf) {
        boolean hasTarget = buf.readBoolean();
        BlockPos target = hasTarget ? buf.readBlockPos() : null;
        return new ClientboundOvervaultCompassPacket(target);
    }

    public static void handle(ClientboundOvervaultCompassPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Must be handled on client thread
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (packet.hasTarget) {
                    OvervaultCompassHandler.setTarget(packet.target);
                } else {
                    OvervaultCompassHandler.clearTarget();
                }
            });
        });
        context.setPacketHandled(true);
    }
}
