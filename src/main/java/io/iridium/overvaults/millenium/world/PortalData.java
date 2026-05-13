package io.iridium.overvaults.millenium.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.UUID;

public class PortalData {
    public static final String DEFAULT_LOGIN_TRANSLATION_COMPONENT = "overvaults.portal.login";
    public static final String DEFAULT_DECAY_TRANSLATION_COMPONENT = "overvaults.portal.decay";

    private final Rotation rotation;
    private final BlockPos portalFrameCenterPos;
    private final StructureSize size;
    private final ResourceKey<Level> dimension;
    private boolean activated;
    private int modifiersRemoved;
    private int secondsUntilDecay;
    private int activeTicks;
    private UUID activeVaultId;
    private String loginTranslationComponent;
    private String decayTranslationComponent;

    public PortalData(Rotation rotation, BlockPos portalFrameCenterPos, StructureSize size, ResourceKey<Level> dimension, boolean activated, int modifiersRemoved) {
        this(rotation, portalFrameCenterPos, size, dimension, activated, modifiersRemoved, -1, 0, null, DEFAULT_LOGIN_TRANSLATION_COMPONENT, DEFAULT_DECAY_TRANSLATION_COMPONENT);
    }

    public PortalData(Rotation rotation, BlockPos portalFrameCenterPos, StructureSize size, ResourceKey<Level> dimension, boolean activated, int modifiersRemoved, int secondsUntilDecay, int activeTicks, String loginTranslationComponent, String decayTranslationComponent) {
        this(rotation, portalFrameCenterPos, size, dimension, activated, modifiersRemoved, secondsUntilDecay, activeTicks, null, loginTranslationComponent, decayTranslationComponent);
    }

    public PortalData(Rotation rotation, BlockPos portalFrameCenterPos, StructureSize size, ResourceKey<Level> dimension, boolean activated, int modifiersRemoved, int secondsUntilDecay, int activeTicks, UUID activeVaultId, String loginTranslationComponent, String decayTranslationComponent) {
        this.rotation = rotation;
        this.portalFrameCenterPos = portalFrameCenterPos;
        this.size = size;
        this.activated = activated;
        this.dimension = dimension;
        this.modifiersRemoved = modifiersRemoved;
        this.secondsUntilDecay = secondsUntilDecay;
        this.activeTicks = activeTicks;
        this.activeVaultId = activeVaultId;
        this.loginTranslationComponent = loginTranslationComponent == null ? DEFAULT_LOGIN_TRANSLATION_COMPONENT : loginTranslationComponent;
        this.decayTranslationComponent = decayTranslationComponent == null ? DEFAULT_DECAY_TRANSLATION_COMPONENT : decayTranslationComponent;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public BlockPos getPortalFrameCenterPos() {
        return portalFrameCenterPos;
    }

    public StructureSize getSize() {
        return size;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public boolean getActiveState() {
        return activated;
    }

    public void setActiveState(boolean state) {
        activated = state;
        if (!state) {
            activeTicks = 0;
            activeVaultId = null;
            secondsUntilDecay = -1;
            loginTranslationComponent = DEFAULT_LOGIN_TRANSLATION_COMPONENT;
            decayTranslationComponent = DEFAULT_DECAY_TRANSLATION_COMPONENT;
        }
    }

    public int getModifiersRemoved() {
        return modifiersRemoved;
    }

    public void setModifiersRemoved(int modifiersRemoved) {
        this.modifiersRemoved = modifiersRemoved;
    }

    public void addModifiersRemoved(int added) {
        modifiersRemoved += added;
    }

    public int getSecondsUntilDecay() {
        return secondsUntilDecay;
    }

    public int getActiveTicks() {
        return activeTicks;
    }

    public void addActiveTick() {
        activeTicks++;
    }

    public UUID getActiveVaultId() {
        return activeVaultId;
    }

    public void setActiveVaultId(UUID activeVaultId) {
        this.activeVaultId = activeVaultId;
    }

    public String getLoginTranslationComponent() {
        return loginTranslationComponent;
    }

    public String getDecayTranslationComponent() {
        return decayTranslationComponent;
    }

    public void setActivePortalConfig(int secondsUntilDecay, String loginTranslationComponent, String decayTranslationComponent) {
        this.secondsUntilDecay = secondsUntilDecay;
        this.activeTicks = 0;
        this.loginTranslationComponent = loginTranslationComponent == null ? DEFAULT_LOGIN_TRANSLATION_COMPONENT : loginTranslationComponent;
        this.decayTranslationComponent = decayTranslationComponent == null ? DEFAULT_DECAY_TRANSLATION_COMPONENT : decayTranslationComponent;
    }

    public boolean shouldDecayFromTimer() {
        return secondsUntilDecay >= 0 && activeTicks >= secondsUntilDecay * 20;
    }

    //Portals that have different Active States will be considered equal
    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;

        PortalData data = (PortalData) obj;

        if (!rotation.equals(data.rotation)) return false;
        if (!portalFrameCenterPos.equals(data.portalFrameCenterPos)) return false;
        if (!size.equals(data.size)) return false;
        return dimension.equals(data.dimension);
    }

    @Override
    public int hashCode() {
        int result = rotation.hashCode();
        result = 31 * result + portalFrameCenterPos.hashCode();
        result = 31 * result + size.hashCode();
        result = 31 * result + dimension.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PortalData{" +
                "rotation=" + rotation +
                ", portalFrameCenterPos=" + portalFrameCenterPos +
                ", size=" + size +
                ", dimension=" + dimension +
                ", activated=" + activated +
                ", modifiersRemoved=" + modifiersRemoved +
                ", secondsUntilDecay=" + secondsUntilDecay +
                ", activeTicks=" + activeTicks +
                ", activeVaultId=" + activeVaultId +
                ", loginTranslationComponent='" + loginTranslationComponent + '\'' +
                ", decayTranslationComponent='" + decayTranslationComponent + '\'' +
                '}';
    }
}
