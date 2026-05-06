package io.iridium.overvaults.millenium.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

public class ActiveCrystalSavedData extends SavedData {
    private static final String DATA_NAME = "overvaults_active_crystal";

    private boolean active;
    private BlockPos pedestalPos = BlockPos.ZERO;
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private int portalIndex = -1;
    private int secondsUntilDecay = -1;
    private int activeTicks;
    private String decayTranslationComponent = PortalData.DEFAULT_DECAY_TRANSLATION_COMPONENT;
    private String claimCrystalTitle = "OverVault Crystal";

    public boolean hasActiveCrystal() {
        return active;
    }

    public BlockPos getPedestalPos() {
        return pedestalPos;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public int getPortalIndex() {
        return portalIndex;
    }

    public int getSecondsUntilDecay() {
        return secondsUntilDecay;
    }

    public int getActiveTicks() {
        return activeTicks;
    }

    public void addActiveTick() {
        activeTicks++;
        setDirty();
    }

    public String getDecayTranslationComponent() {
        return decayTranslationComponent;
    }

    public String getClaimCrystalTitle() {
        return claimCrystalTitle;
    }

    public void setActiveCrystal(BlockPos pedestalPos, ResourceKey<Level> dimension, int portalIndex, int secondsUntilDecay, String decayTranslationComponent, String claimCrystalTitle) {
        this.active = true;
        this.pedestalPos = pedestalPos.immutable();
        this.dimension = dimension;
        this.portalIndex = portalIndex;
        this.secondsUntilDecay = secondsUntilDecay;
        this.activeTicks = 0;
        this.decayTranslationComponent = decayTranslationComponent == null ? PortalData.DEFAULT_DECAY_TRANSLATION_COMPONENT : decayTranslationComponent;
        this.claimCrystalTitle = claimCrystalTitle == null || claimCrystalTitle.isBlank() ? "OverVault Crystal" : claimCrystalTitle;
        setDirty();
    }

    public void clearActiveCrystal() {
        this.active = false;
        this.pedestalPos = BlockPos.ZERO;
        this.dimension = Level.OVERWORLD;
        this.portalIndex = -1;
        this.secondsUntilDecay = -1;
        this.activeTicks = 0;
        this.decayTranslationComponent = PortalData.DEFAULT_DECAY_TRANSLATION_COMPONENT;
        this.claimCrystalTitle = "OverVault Crystal";
        setDirty();
    }

    public boolean shouldDecayFromTimer() {
        return active && secondsUntilDecay >= 0 && activeTicks >= secondsUntilDecay * 20;
    }

    public static ActiveCrystalSavedData load(CompoundTag nbt) {
        ActiveCrystalSavedData data = new ActiveCrystalSavedData();
        data.active = nbt.getBoolean("Active");
        data.pedestalPos = nbt.contains("PedestalPos") ? BlockPos.of(nbt.getLong("PedestalPos")) : BlockPos.ZERO;
        data.dimension = nbt.contains("Dimension")
                ? ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(nbt.getString("Dimension")))
                : Level.OVERWORLD;
        data.portalIndex = nbt.getInt("PortalIndex");
        data.secondsUntilDecay = nbt.contains("SecondsUntilDecay") ? nbt.getInt("SecondsUntilDecay") : -1;
        data.activeTicks = nbt.getInt("ActiveTicks");
        data.decayTranslationComponent = nbt.contains("DecayTranslationComponent")
                ? nbt.getString("DecayTranslationComponent")
                : PortalData.DEFAULT_DECAY_TRANSLATION_COMPONENT;
        data.claimCrystalTitle = nbt.contains("ClaimCrystalTitle")
                ? nbt.getString("ClaimCrystalTitle")
                : "OverVault Crystal";
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putBoolean("Active", active);
        nbt.putLong("PedestalPos", pedestalPos.asLong());
        nbt.putString("Dimension", dimension.location().toString());
        nbt.putInt("PortalIndex", portalIndex);
        nbt.putInt("SecondsUntilDecay", secondsUntilDecay);
        nbt.putInt("ActiveTicks", activeTicks);
        nbt.putString("DecayTranslationComponent", decayTranslationComponent);
        nbt.putString("ClaimCrystalTitle", claimCrystalTitle);
        return nbt;
    }

    public static ActiveCrystalSavedData getServer() {
        return get(ServerLifecycleHooks.getCurrentServer());
    }

    public static ActiveCrystalSavedData get(ServerLevel level) {
        return get(level.getServer());
    }

    public static ActiveCrystalSavedData get(MinecraftServer srv) {
        return srv.overworld().getDataStorage().computeIfAbsent(ActiveCrystalSavedData::load, ActiveCrystalSavedData::new, DATA_NAME);
    }
}
