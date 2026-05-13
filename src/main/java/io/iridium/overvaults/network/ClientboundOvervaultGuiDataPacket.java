package io.iridium.overvaults.network;

import io.iridium.overvaults.client.OvervaultPortalClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientboundOvervaultGuiDataPacket {
    public final boolean dataAvailable;
    public final boolean active;
    public final String dimensionId;
    public final String rank;
    public final int rankColor;
    public final int vaultTimerTicks;
    public final int vaultLevel;
    public final List<PlayerEntry> players;
    public final int portalActiveTicks;
    public final String errorMessage;

    public ClientboundOvervaultGuiDataPacket(boolean dataAvailable, boolean active, String dimensionId, String rank, int rankColor, int vaultTimerTicks, int vaultLevel, List<PlayerEntry> players, int portalActiveTicks, String errorMessage) {
        this.dataAvailable = dataAvailable;
        this.active = active;
        this.dimensionId = dimensionId;
        this.rank = rank;
        this.rankColor = rankColor;
        this.vaultTimerTicks = vaultTimerTicks;
        this.vaultLevel = vaultLevel;
        this.players = List.copyOf(players);
        this.portalActiveTicks = portalActiveTicks;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static ClientboundOvervaultGuiDataPacket unavailable(String errorMessage) {
        return new ClientboundOvervaultGuiDataPacket(false, false, "inactive", "Unknown", 0x00AAAA, 30000, -1, List.of(), 0, errorMessage);
    }

    public static void encode(ClientboundOvervaultGuiDataPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.dataAvailable);
        buf.writeBoolean(packet.active);
        buf.writeUtf(packet.dimensionId);
        buf.writeUtf(packet.rank);
        buf.writeInt(packet.rankColor);
        buf.writeInt(packet.vaultTimerTicks);
        buf.writeInt(packet.vaultLevel);
        buf.writeInt(packet.players.size());
        for (PlayerEntry player : packet.players) {
            buf.writeInt(player.vaultLevel());
            buf.writeUtf(player.name());
        }
        buf.writeInt(packet.portalActiveTicks);
        buf.writeUtf(packet.errorMessage);
    }

    public static ClientboundOvervaultGuiDataPacket decode(FriendlyByteBuf buf) {
        boolean dataAvailable = buf.readBoolean();
        boolean active = buf.readBoolean();
        String dimensionId = buf.readUtf();
        String rank = buf.readUtf();
        int rankColor = buf.readInt();
        int vaultTimerTicks = buf.readInt();
        int vaultLevel = buf.readInt();
        int playerCount = buf.readInt();
        List<PlayerEntry> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            players.add(new PlayerEntry(buf.readInt(), buf.readUtf()));
        }
        int portalActiveTicks = buf.readInt();
        String errorMessage = buf.readUtf();
        return new ClientboundOvervaultGuiDataPacket(dataAvailable, active, dimensionId, rank, rankColor, vaultTimerTicks, vaultLevel, players, portalActiveTicks, errorMessage);
    }

    public static void handle(ClientboundOvervaultGuiDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> OvervaultPortalClientHandler.handle(packet)));
        context.setPacketHandled(true);
    }

    public record PlayerEntry(int vaultLevel, String name) {
    }
}
