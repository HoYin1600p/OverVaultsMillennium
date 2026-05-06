package io.iridium.overvaults.millenium.event;

import io.iridium.overvaults.config.VaultConfigRegistry;
import io.iridium.overvaults.millenium.util.MiscUtil;
import io.iridium.overvaults.millenium.util.OverVaultCrystalUtil;
import io.iridium.overvaults.millenium.world.ActiveCrystalSavedData;
import io.iridium.overvaults.millenium.world.PortalData;
import io.iridium.overvaults.millenium.world.PortalSavedData;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
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
            if (data != null) {
                if (data.getDimension().equals(player.getLevel().dimension()) && VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.UPDATE_VAULT_COMPASS) {
                    MiscUtil.sendCompassInfoToPlayer(player, data.getDimension(), data.getPortalFrameCenterPos());
                }

                player.sendMessage(MiscUtil.getPortalMessage(data.getLoginTranslationComponent(), data.getDimension()), ChatType.SYSTEM, Util.NIL_UUID);
                return;
            }

            ActiveCrystalSavedData activeCrystalData = ActiveCrystalSavedData.getServer();
            if (activeCrystalData.hasActiveCrystal()
                    && activeCrystalData.getDimension().equals(player.getLevel().dimension())
                    && VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.UPDATE_VAULT_COMPASS) {
                BlockPos pedestalPos = OverVaultCrystalUtil.getActiveCrystalPedestalPos(player.getServer(), activeCrystalData).orElse(activeCrystalData.getPedestalPos());
                MiscUtil.sendCompassInfoToPlayer(player, activeCrystalData.getDimension(), pedestalPos);
            }
        }
    }
}
