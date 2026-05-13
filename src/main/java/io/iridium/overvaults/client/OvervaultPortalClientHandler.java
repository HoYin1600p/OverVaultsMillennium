package io.iridium.overvaults.client;

import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.client.gui.OvervaultPortalScreen;
import io.iridium.overvaults.network.ClientboundOvervaultGuiDataPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextComponent;

public class OvervaultPortalClientHandler {
    public static void handle(ClientboundOvervaultGuiDataPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (!packet.dataAvailable) {
                OverVaults.LOGGER.error("OverVault GUI data unavailable: {}", packet.errorMessage);
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(new TextComponent(packet.errorMessage), true);
                }
            }

            if (minecraft.screen instanceof OvervaultPortalScreen screen) {
                screen.applyData(packet);
            }
        });
    }
}
