package io.iridium.overvaults.client;

import iskallia.vault.core.event.ClientEvents;
import iskallia.vault.core.vault.ClientVaults;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OvervaultCompassHandler {

    private static BlockPos overvaultTarget = null;
    private static boolean initialized = false;

    public static void setTarget(BlockPos target) {
        overvaultTarget = target;
    }


    public static void clearTarget() {
        overvaultTarget = null;
    }

    public static BlockPos getTarget() {
        return overvaultTarget;
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientEvents.COMPASS_PROPERTY.register(OvervaultCompassHandler.class, data -> {
            if (overvaultTarget != null && ClientVaults.getActive().isEmpty()) {
                data.setTarget(overvaultTarget);
            }
        });
    }
}
