package io.iridium.overvaults.millenium.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PortalSavedData extends SavedData {
    private static final String DATA_NAME = "overvaults_stored_structures";
    private final List<PortalData> portalDataList = new ArrayList<>();

    public void addPortalData(PortalData newPortalData) {
        // Check if the new portal data is already in the list
        boolean exists = portalDataList.stream().anyMatch(existingPortalData -> existingPortalData.equals(newPortalData));

        if (!exists) {
            portalDataList.add(newPortalData);
            setDirty();
        }
    }

    public List<PortalData> getPortalData() {
        return portalDataList;
    }

    public void removePortalData(int index) {
        portalDataList.remove(index);
        setDirty();
    }

    public boolean hasActiveOverVault() {
        return portalDataList.stream().anyMatch(PortalData::getActiveState);
    }


    public PortalData getFirstActivePortalData() {
        return portalDataList.stream()
                .filter(PortalData::getActiveState)
                .findFirst()
                .orElse(null);
    }

    public static PortalSavedData load(CompoundTag nbt) {
        PortalSavedData data = new PortalSavedData();
        ListTag listTag = nbt.getList("PortalDataList", Tag.TAG_COMPOUND);

        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag portalTag = listTag.getCompound(i);

            Rotation rotation = Rotation.valueOf(portalTag.getString("Rotation"));
            BlockPos portalFrameCenterPos = BlockPos.of(portalTag.getLong("PortalFrameCenterPos"));
            StructureSize size = StructureSize.valueOf(portalTag.getString("Size"));
            ResourceKey<Level> dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(portalTag.getString("Dimension")));
            boolean activeState = portalTag.getBoolean("ActiveState");

            int modifiersRemoved = activeState ? portalTag.getInt("ModifiersRemoved") : -1;
            int secondsUntilDecay = activeState ? portalTag.getInt("SecondsUntilDecay") : -1;
            int activeTicks = activeState ? portalTag.getInt("ActiveTicks") : 0;
            UUID activeVaultId = null;
            if (activeState && portalTag.contains("ActiveVaultId")) {
                try {
                    activeVaultId = UUID.fromString(portalTag.getString("ActiveVaultId"));
                } catch (IllegalArgumentException ignored) {
                    activeVaultId = null;
                }
            }
            String loginTranslationComponent = portalTag.contains("LoginTranslationComponent")
                    ? portalTag.getString("LoginTranslationComponent")
                    : PortalData.DEFAULT_LOGIN_TRANSLATION_COMPONENT;
            String decayTranslationComponent = portalTag.contains("DecayTranslationComponent")
                    ? portalTag.getString("DecayTranslationComponent")
                    : PortalData.DEFAULT_DECAY_TRANSLATION_COMPONENT;

            data.addPortalData(new PortalData(rotation, portalFrameCenterPos, size, dimension, activeState, modifiersRemoved, secondsUntilDecay, activeTicks, activeVaultId, loginTranslationComponent, decayTranslationComponent));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag listTag = new ListTag();

        for (PortalData data : portalDataList) {
            CompoundTag portalTag = new CompoundTag();
            portalTag.putString("Rotation", data.getRotation().name());
            portalTag.putLong("PortalFrameCenterPos", data.getPortalFrameCenterPos().asLong());
            portalTag.putString("Size", data.getSize().name());
            portalTag.putString("Dimension", data.getDimension().location().toString());
            portalTag.putBoolean("ActiveState", data.getActiveState());

            if (data.getActiveState()) {
                portalTag.putInt("ModifiersRemoved", data.getModifiersRemoved());
                portalTag.putInt("SecondsUntilDecay", data.getSecondsUntilDecay());
                portalTag.putInt("ActiveTicks", data.getActiveTicks());
                if (data.getActiveVaultId() != null) {
                    portalTag.putString("ActiveVaultId", data.getActiveVaultId().toString());
                }
                portalTag.putString("LoginTranslationComponent", data.getLoginTranslationComponent());
                portalTag.putString("DecayTranslationComponent", data.getDecayTranslationComponent());
            }


            listTag.add(portalTag);
        }
        nbt.put("PortalDataList", listTag);
        return nbt;
    }

    // Data-getters
    public static PortalSavedData getServer() {
        return get(ServerLifecycleHooks.getCurrentServer());
    }

    public static PortalSavedData get(ServerLevel level) {
        return get(level.getServer());
    }

    public static PortalSavedData get(MinecraftServer srv) {
        return srv.overworld().getDataStorage().computeIfAbsent(PortalSavedData::load, PortalSavedData::new, DATA_NAME);
    }
}
