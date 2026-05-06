package io.iridium.overvaults.millenium.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

public class OverVaultsPerformanceSavedData extends SavedData {
    private static final String DATA_NAME = "overvaults_performance_mode";

    private Mode mode = Mode.PORTAL;

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.PORTAL : mode;
        setDirty();
    }

    public static OverVaultsPerformanceSavedData load(CompoundTag nbt) {
        OverVaultsPerformanceSavedData data = new OverVaultsPerformanceSavedData();
        data.mode = Mode.fromName(nbt.getString("Mode"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putString("Mode", mode.name());
        return nbt;
    }

    public static OverVaultsPerformanceSavedData getServer() {
        return get(ServerLifecycleHooks.getCurrentServer());
    }

    public static OverVaultsPerformanceSavedData get(MinecraftServer srv) {
        return srv.overworld().getDataStorage().computeIfAbsent(OverVaultsPerformanceSavedData::load, OverVaultsPerformanceSavedData::new, DATA_NAME);
    }

    public enum Mode {
        PORTAL,
        CRYSTAL;

        public static Mode fromName(String name) {
            for (Mode mode : values()) {
                if (mode.name().equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            return PORTAL;
        }
    }
}
