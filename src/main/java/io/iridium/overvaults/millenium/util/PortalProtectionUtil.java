package io.iridium.overvaults.millenium.util;

import io.iridium.overvaults.millenium.world.PortalData;
import io.iridium.overvaults.millenium.world.PortalSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

public class PortalProtectionUtil {
    public static boolean isProtectedActivePortalBlock(LevelAccessor level, BlockPos pos) {
        return getProtectedActivePortalData(level, pos) != null;
    }

    public static PortalData getProtectedActivePortalData(LevelAccessor level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        PortalData activePortalData = PortalSavedData.get(serverLevel).getFirstActivePortalData();
        if (activePortalData == null || !activePortalData.getDimension().equals(serverLevel.dimension())) {
            return null;
        }

        for (BlockPos portalPos : activePortalData.getSize().getBlockPositions(activePortalData.getPortalFrameCenterPos(), activePortalData.getRotation())) {
            if (portalPos.equals(pos)) {
                return activePortalData;
            }
        }

        return null;
    }
}
