package io.iridium.overvaults.millenium.gui;

import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.network.ClientboundOvervaultGuiDataPacket;
import io.iridium.overvaults.network.OverVaultsNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class OvervaultGuiUpdateManager {
    private static final int UPDATE_INTERVAL_TICKS = 100;
    private static final Set<UUID> VIEWERS = new HashSet<>();
    private static int updateTicks;

    public static void open(ServerPlayer player) {
        VIEWERS.add(player.getUUID());
        sendSnapshot(player);
    }

    public static void close(UUID playerId) {
        VIEWERS.remove(playerId);
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || VIEWERS.isEmpty()) {
            return;
        }

        updateTicks++;
        if (updateTicks < UPDATE_INTERVAL_TICKS) {
            return;
        }

        updateTicks = 0;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            VIEWERS.clear();
            return;
        }

        Iterator<UUID> iterator = VIEWERS.iterator();
        while (iterator.hasNext()) {
            UUID playerId = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null || player.hasDisconnected()) {
                iterator.remove();
                continue;
            }

            sendSnapshot(player);
        }
    }

    private static void sendSnapshot(ServerPlayer player) {
        ClientboundOvervaultGuiDataPacket packet;
        try {
            packet = OvervaultGuiStatusBuilder.build(player.getServer());
        } catch (Exception exception) {
            OverVaults.LOGGER.error("Failed to build OverVault GUI data for {}", player.getGameProfile().getName(), exception);
            packet = ClientboundOvervaultGuiDataPacket.unavailable("OverVault GUI data is unavailable. Check the server log for details.");
        }

        OverVaultsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
