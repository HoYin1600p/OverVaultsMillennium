package io.iridium.overvaults.millenium.event;

import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.millenium.util.PortalProtectionUtil;
import io.iridium.overvaults.millenium.util.PortalUtil;
import io.iridium.overvaults.millenium.world.PortalData;
import iskallia.vault.init.ModBlocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.world.BlockEvent;

public class PortalProtectionEventHandler {
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getWorld() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!event.getState().is(ModBlocks.VAULT_PORTAL)) {
            return;
        }

        PortalData activePortalData = PortalProtectionUtil.getProtectedActivePortalData(serverLevel, event.getPos());
        if (activePortalData == null) {
            return;
        }

        Player player = event.getPlayer();
        if (player.isCreative()) {
            MinecraftServer server = serverLevel.getServer();
            OverVaults.LOGGER.info(
                    "Creative player {} intentionally broke active OverVault portal at {} in {}. Deactivating portal.",
                    player.getName().getString(),
                    activePortalData.getPortalFrameCenterPos(),
                    activePortalData.getDimension().location()
            );
            PortalUtil.deactivatePortal(server, activePortalData, true, null);
            return;
        }

        event.setCanceled(true);
    }

    public static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (PortalProtectionUtil.isProtectedActivePortalBlock(event.getWorld(), event.getPos())) {
            event.setNewState(event.getOriginalState());
        }
    }
}
