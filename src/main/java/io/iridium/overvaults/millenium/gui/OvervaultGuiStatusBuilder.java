package io.iridium.overvaults.millenium.gui;

import io.iridium.overvaults.millenium.util.PortalRankTextUtil;
import io.iridium.overvaults.millenium.world.PortalData;
import io.iridium.overvaults.millenium.world.PortalSavedData;
import io.iridium.overvaults.network.ClientboundOvervaultGuiDataPacket;
import iskallia.vault.core.vault.Vault;
import iskallia.vault.core.vault.time.TickClock;
import iskallia.vault.world.data.PlayerVaultStatsData;
import iskallia.vault.world.data.ServerVaults;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OvervaultGuiStatusBuilder {
    private static final int DEFAULT_VAULT_TIME_TICKS = 30000;

    public static ClientboundOvervaultGuiDataPacket build(MinecraftServer server) {
        PortalData activePortal = PortalSavedData.get(server).getFirstActivePortalData();
        if (activePortal == null) {
            return new ClientboundOvervaultGuiDataPacket(true, false, "inactive", "Unknown", 0x00AAAA, DEFAULT_VAULT_TIME_TICKS, -1, List.of(), 0, "");
        }

        PortalRankTextUtil.PortalRankInfo rankInfo = PortalRankTextUtil.fromPortalOpenText(activePortal.getOpenTranslationComponent());
        Optional<Vault> activeVault = getActiveVault(activePortal);
        int vaultTimerTicks = activeVault
                .map(vault -> vault.get(Vault.CLOCK).get(TickClock.DISPLAY_TIME))
                .orElse(DEFAULT_VAULT_TIME_TICKS);
        int vaultLevel = activeVault
                .map(vault -> vault.get(Vault.LEVEL).get())
                .orElse(-1);

        List<ClientboundOvervaultGuiDataPacket.PlayerEntry> players = getPlayersInVault(server, activePortal.getActiveVaultId());

        return new ClientboundOvervaultGuiDataPacket(
                true,
                true,
                activePortal.getDimension().location().toString(),
                rankInfo.rank(),
                rankInfo.color(),
                vaultTimerTicks,
                vaultLevel,
                players,
                activePortal.getActiveTicks(),
                ""
        );
    }

    private static Optional<Vault> getActiveVault(PortalData activePortal) {
        UUID activeVaultId = activePortal.getActiveVaultId();
        if (activeVaultId == null) {
            return Optional.empty();
        }

        return ServerVaults.get(activeVaultId);
    }

    private static List<ClientboundOvervaultGuiDataPacket.PlayerEntry> getPlayersInVault(MinecraftServer server, UUID activeVaultId) {
        if (activeVaultId == null) {
            return List.of();
        }

        PlayerVaultStatsData statsData = PlayerVaultStatsData.get(server);
        List<ClientboundOvervaultGuiDataPacket.PlayerEntry> players = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Optional<Vault> playerVault = ServerVaults.get(player.getLevel());
            if (playerVault.isEmpty() || !activeVaultId.equals(playerVault.get().get(Vault.ID))) {
                continue;
            }

            int vaultLevel = statsData.getVaultStats(player).getVaultLevel();
            players.add(new ClientboundOvervaultGuiDataPacket.PlayerEntry(vaultLevel, player.getGameProfile().getName()));
        }

        players.sort(Comparator.comparing(ClientboundOvervaultGuiDataPacket.PlayerEntry::name, String.CASE_INSENSITIVE_ORDER));
        return players;
    }
}
