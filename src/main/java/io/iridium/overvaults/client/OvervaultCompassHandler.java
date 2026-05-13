package io.iridium.overvaults.client;

import iskallia.vault.core.event.ClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OvervaultCompassHandler {

    private static BlockPos overvaultTarget = null;
    private static ResourceKey<Level> overvaultTargetDimension = null;
    private static boolean initialized = false;

    public static void setTarget(BlockPos target) {
        setTarget(null, target);
    }

    public static void setTarget(ResourceKey<Level> dimension, BlockPos target) {
        overvaultTargetDimension = dimension;
        overvaultTarget = target;
    }


    public static void clearTarget() {
        overvaultTargetDimension = null;
        overvaultTarget = null;
    }

    public static BlockPos getTarget() {
        return overvaultTarget;
    }

    public static boolean hasTargetFor(Level level) {
        return overvaultTarget != null
                && (overvaultTargetDimension == null || level.dimension().equals(overvaultTargetDimension));
    }

    public static boolean shouldApplyOvervaultTarget(Level level) {
        return level != null && !level.dimension().location().getNamespace().equals("the_vault");
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        ClientEvents.COMPASS_PROPERTY.register(OvervaultCompassHandler.class, data -> {
            if (shouldApplyOvervaultTarget(data.getWorld()) && hasTargetFor(data.getWorld())) {
                data.setTarget(overvaultTarget);
            }
        });
    }
}
