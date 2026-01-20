package io.iridium.overvaults.millenium.event;

import io.iridium.overvaults.config.VaultConfigRegistry;
import io.iridium.overvaults.millenium.util.MiscUtil;
import io.iridium.overvaults.millenium.util.TextUtil;
import io.iridium.overvaults.millenium.world.PortalData;
import io.iridium.overvaults.millenium.world.PortalSavedData;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public class OnPlayerLogin {
    @SubscribeEvent
    public static void onPlayerLoginEvent(PlayerEvent.PlayerLoggedInEvent event) {
        if (!VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.BROADCAST_IN_CHAT) return;

        if (event.getPlayer() instanceof ServerPlayer player) {
            PortalData data = PortalSavedData.getServer().getFirstActivePortalData();
            if (data != null && data.getDimension() == player.getLevel().dimension()) {
                if (VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.UPDATE_VAULT_COMPASS) {
                    MiscUtil.sendCompassInfoToPlayer(player, data.getPortalFrameCenterPos());
                }
            }

            player.sendMessage(TextUtil.loginComponent(), ChatType.SYSTEM, Util.NIL_UUID);
        }
    }
}
