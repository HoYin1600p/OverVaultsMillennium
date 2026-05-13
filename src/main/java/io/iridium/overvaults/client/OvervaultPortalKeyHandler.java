package io.iridium.overvaults.client;

import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.client.gui.OvervaultPortalScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OverVaults.MOD_ID, value = Dist.CLIENT)
public class OvervaultPortalKeyHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (ModKeybinds.openOvervaultGui == null || !ModKeybinds.openOvervaultGui.consumeClick()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new OvervaultPortalScreen());
        }
    }
}
