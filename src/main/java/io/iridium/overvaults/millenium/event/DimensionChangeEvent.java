package io.iridium.overvaults.millenium.event;

import io.iridium.overvaults.config.VaultConfigRegistry;
import io.iridium.overvaults.millenium.util.MiscUtil;
import io.iridium.overvaults.millenium.world.PortalData;
import io.iridium.overvaults.millenium.world.PortalSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public class DimensionChangeEvent {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getPlayer() == null) return;
        if (ServerLifecycleHooks.getCurrentServer() == null) return;
        if (!VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.UPDATE_VAULT_COMPASS) return;

        if (event.getPlayer() instanceof ServerPlayer player) {
            PortalData data = PortalSavedData.getServer().getFirstActivePortalData();
            if (data == null) {
                MiscUtil.clearCompassInfoForPlayer(player);
                return;
            }
            if (data.getDimension() == event.getTo()) {
                MiscUtil.sendCompassInfoToPlayer(player, data.getPortalFrameCenterPos());
            } else {
                MiscUtil.clearCompassInfoForPlayer(player);
            }
        }
    }
}
