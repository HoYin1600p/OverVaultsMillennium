package io.iridium.overvaults.millenium.util;

import io.iridium.overvaults.config.VaultConfigRegistry;
import io.iridium.overvaults.millenium.world.PortalData;
import io.iridium.overvaults.network.ClientboundOvervaultCompassPacket;
import io.iridium.overvaults.network.OverVaultsNetwork;
import iskallia.vault.world.data.ServerVaults;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

public class MiscUtil {


    public static void sendCompassInfoToPlayer(ServerPlayer player, BlockPos pos) {
        OverVaultsNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ClientboundOvervaultCompassPacket(pos)
        );
    }

    public static void clearCompassInfoForPlayer(ServerPlayer player) {
        OverVaultsNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                ClientboundOvervaultCompassPacket.clear()
        );
    }

    public static void sendCompassInfo(ServerLevel level, BlockPos pos) {
        level.getPlayers(player -> !ServerVaults.isInVault(player.getLevel()))
                .forEach(player -> sendCompassInfoToPlayer(player, pos));
    }

    /**
     * Clears compass info for all players in the given level who are not currently in a vault.
     */
    public static void clearCompassInfo(ServerLevel level) {
        level.getPlayers(player -> !ServerVaults.isInVault(player.getLevel()))
                .forEach(MiscUtil::clearCompassInfoForPlayer);
    }

    public static void notifyPlayers(MinecraftServer server, PortalData data, String translationText) {
        server.getPlayerList().getPlayers().forEach(player -> {
            if (VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.PLAY_SOUND_ON_OPEN)
                player.getLevel().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.MASTER, 0.4f, 1.25f);
        });

        if (VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.BROADCAST_IN_CHAT) {
            MiscUtil.broadcast(new TranslatableComponent(translationText, TextUtil.dimensionComponent(data.getDimension())));

            if (VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.UPDATE_VAULT_COMPASS)
                MiscUtil.sendCompassInfo(server.getLevel(data.getDimension()), data.getPortalFrameCenterPos());
        }
    }

    public static int[] convertTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return new int[]{hours, minutes, seconds};
    }

    public static String formatTime(int[] time) {
        return String.format("%02d:%02d:%02d", time[0], time[1], time[2]);
    }

    public static void broadcast(Component message) {
        MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
        if (srv != null) {
            srv.getPlayerList().broadcastMessage(message, ChatType.SYSTEM, Util.NIL_UUID);
        }
    }
}
