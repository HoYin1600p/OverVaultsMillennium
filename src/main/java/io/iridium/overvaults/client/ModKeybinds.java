package io.iridium.overvaults.client;

import io.iridium.overvaults.OverVaults;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;

public class ModKeybinds {
    public static KeyMapping openOvervaultGui;

    public static void register() {
        openOvervaultGui = new KeyMapping(
                "key." + OverVaults.MOD_ID + ".open_overvault_gui",
                -1,
                "key.category." + OverVaults.MOD_ID
        );
        ClientRegistry.registerKeyBinding(openOvervaultGui);
    }
}
