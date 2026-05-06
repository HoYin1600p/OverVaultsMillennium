package io.iridium.overvaults.network;

import io.iridium.overvaults.client.OvervaultCompassHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundOvervaultCompassPacket {
    private final boolean hasTarget;
    private final ResourceKey<Level> dimension;
    private final BlockPos target;

    public ClientboundOvervaultCompassPacket(ResourceKey<Level> dimension, BlockPos target) {
        this.hasTarget = target != null;
        this.dimension = dimension;
        this.target = target;
    }

    public static ClientboundOvervaultCompassPacket clear() {
        return new ClientboundOvervaultCompassPacket(null, null);
    }

    public static void encode(ClientboundOvervaultCompassPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.hasTarget);
        if (packet.hasTarget) {
            buf.writeResourceLocation(packet.dimension.location());
            buf.writeBlockPos(packet.target);
        }
    }

    public static ClientboundOvervaultCompassPacket decode(FriendlyByteBuf buf) {
        boolean hasTarget = buf.readBoolean();
        ResourceKey<Level> dimension = hasTarget
                ? ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation())
                : null;
        BlockPos target = hasTarget ? buf.readBlockPos() : null;
        return new ClientboundOvervaultCompassPacket(dimension, target);
    }

    public static void handle(ClientboundOvervaultCompassPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Must be handled on client thread
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (packet.hasTarget) {
                    OvervaultCompassHandler.setTarget(packet.dimension, packet.target);
                } else {
                    OvervaultCompassHandler.clearTarget();
                }
            });
        });
        context.setPacketHandled(true);
    }
}
