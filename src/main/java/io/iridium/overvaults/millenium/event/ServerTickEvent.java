package io.iridium.overvaults.millenium.event;

import io.iridium.overvaults.OverVaultConstants;
import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.config.VaultConfigRegistry;
import io.iridium.overvaults.millenium.util.MiscUtil;
import io.iridium.overvaults.millenium.util.OverVaultCrystalUtil;
import io.iridium.overvaults.millenium.util.PortalUtil;
import io.iridium.overvaults.millenium.util.TextUtil;
import io.iridium.overvaults.millenium.world.ActiveCrystalSavedData;
import io.iridium.overvaults.millenium.world.BlockEntityChunkSavedData;
import io.iridium.overvaults.millenium.world.PortalData;
import io.iridium.overvaults.millenium.world.PortalSavedData;
import iskallia.vault.block.entity.VaultPortalTileEntity;
import iskallia.vault.core.vault.modifier.VaultModifierStack;
import iskallia.vault.init.ModBlocks;
import iskallia.vault.world.data.ServerVaults;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class ServerTickEvent {
    public static int counter = 0; // Counter used to track the time to the next Portal Spawn

    public static int actlTicksForPortalSpawn = -1; // Field to track which time is required for the next Vault Portal Spawn
    public static int activePortalTickCounter = 0; // Field to track the time an OverVault is active
    public static int actlRemoveModifierTimer = -1; // Field to track which time is required for the next Modifer Portal Removal

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if(level == null) return;

        PortalSavedData portalSavedData = PortalSavedData.get(level);
        ActiveCrystalSavedData activeCrystalData = ActiveCrystalSavedData.get(level);
        if(actlTicksForPortalSpawn == -1) actlTicksForPortalSpawn = getRandomTicksForPortalSpawn();

        // Check if the counter has reached the limit for portal spawning
        if (shouldSpawnPortal(actlTicksForPortalSpawn) && !portalSavedData.hasActiveOverVault() && !activeCrystalData.hasActiveCrystal()) {
            ResourceKey<Level> preferredDimension = VaultConfigRegistry.OVERVAULTS_PORTAL_CONFIG.getLevel(server).dimension();
            List<PortalData> portalDataList = new ArrayList<>(portalSavedData.getPortalData());
            Collections.shuffle(portalDataList);

            if (!portalDataList.isEmpty()) {
                boolean portalActivated = false;
                boolean foundPortalOutsideRange = false;

                // First pass: Look for portals in the preferred dimension
                for (PortalData data : portalDataList) {
                    if (!data.getDimension().equals(preferredDimension)) {
                        continue; // Skip portals not in the preferred dimension
                    }

                    if(data.getPortalFrameCenterPos().getY() < 64 && data.getDimension().equals(Level.OVERWORLD)) {
                        continue; // Skip portals that are lower than y 64 and in the Overworld
                    }


                    ServerLevel portalLevel = server.getLevel(data.getDimension());
                    if (portalLevel == null) {
                        OverVaults.LOGGER.error("Level {} equals null. Please report this.", data.getDimension());
                        continue; // Skip this portal
                    }

                    if(VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.RESPECT_WORLD_BORDER) {
                        WorldBorder worldBorder = level.getWorldBorder();
                        if(!worldBorder.isWithinBounds(data.getPortalFrameCenterPos())) {
                            continue; // If the border is not within the portal bounds; skip this portal
                        }
                    }

                    if(VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.getSpawnRadiusForLevel(portalLevel.dimension()) != -1) {
                        BlockPos portalFrameCenterPos = data.getPortalFrameCenterPos();
                        double distance = Math.sqrt(Math.pow(portalFrameCenterPos.getX(), 2) + Math.pow(portalFrameCenterPos.getZ(), 2)); // Calculate distance to center of the world

                        if (!(distance <= VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.getSpawnRadiusForLevel(portalLevel.dimension()))) {
                            foundPortalOutsideRange = true;
                            continue; // Ignore if it's not in the radius
                        }
                    }



                    if (PortalUtil.activateOverVault(server, data)) {
                        actlTicksForPortalSpawn = getRandomTicksForPortalSpawn();
                        counter = 0;
                        portalActivated = true;
                        break; // Successfully activated a portal, no need to check others
                    }
                }

                // Second pass: If no portals were activated in the preferred dimension, check any dimension
                if (!portalActivated) {
                    OverVaults.LOGGER.warn("Did not find a valid Portal in the preferred dimension");
                    for (PortalData data : portalDataList) {
                        if (data.getPortalFrameCenterPos().getY() < 64) {
                            continue; // Skip portals that are lower than y 64
                        }

                        ServerLevel portalLevel = server.getLevel(data.getDimension());
                        if (portalLevel == null) {
                            OverVaults.LOGGER.error("Level {} equals null on second pass? Please report this.", data.getDimension());
                            continue; // Skip this portal
                        }

                        if (VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.RESPECT_WORLD_BORDER) {
                            WorldBorder worldBorder = level.getWorldBorder();
                            if (!worldBorder.isWithinBounds(data.getPortalFrameCenterPos())) {
                                continue; // If the border is not within the portal bounds; skip this portal
                            }
                        }

                        if (VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.getSpawnRadiusForLevel(portalLevel.dimension()) != -1) {
                            BlockPos portalFrameCenterPos = data.getPortalFrameCenterPos();
                            double distance = Math.sqrt(Math.pow(portalFrameCenterPos.getX(), 2) + Math.pow(portalFrameCenterPos.getZ(), 2)); // Calculate distance to center of the world

                            if (!(distance <= VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.getSpawnRadiusForLevel(portalLevel.dimension()))) {
                                foundPortalOutsideRange = true;
                                continue; // Ignore if it's not in the radius
                            }
                        }

                        if (PortalUtil.activateOverVault(server, data)) {
                            actlTicksForPortalSpawn = getRandomTicksForPortalSpawn();
                            counter = 0;
                            portalActivated = true;
                            break; // Successfully activated a portal, no need to check others
                        }
                    }
                }

                if (!portalActivated && foundPortalOutsideRange) {
                    OverVaults.LOGGER.warn("Only found valid portal(s) outside the configured dimension ranges. Reset Counter");
                    counter = 0;
                } else if (!portalActivated) {
                    OverVaults.LOGGER.warn("No valid portals were found to activate. Reset Counter");
                }
            }
        }


        // Increment the counter only if there's no active portal across all dimensions
        if (!PortalSavedData.get(level).hasActiveOverVault() && !activeCrystalData.hasActiveCrystal() && counter < Integer.MAX_VALUE) {
            counter++;
        }

        if (activeCrystalData.hasActiveCrystal()) {
            if (!OverVaultCrystalUtil.activeCrystalStillExists(server, activeCrystalData)) {
                OverVaults.LOGGER.info(
                        "Active OverVault crystal at {} in {} no longer exists. Clearing active crystal state.",
                        activeCrystalData.getPedestalPos(),
                        activeCrystalData.getDimension().location()
                );
                activeCrystalData.clearActiveCrystal();
                OverVaultCrystalUtil.clearCompassInfo(server);
                activePortalTickCounter = 0;
                actlRemoveModifierTimer = -1;
                return;
            }

            activeCrystalData.addActiveTick();

            if (VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.UPDATE_VAULT_COMPASS && activeCrystalData.getActiveTicks() % 20 == 0) {
                BlockPos pedestalPos = OverVaultCrystalUtil.getActiveCrystalPedestalPos(server, activeCrystalData).orElse(activeCrystalData.getPedestalPos());
                MiscUtil.sendCompassInfo(server, activeCrystalData.getDimension(), pedestalPos);
            }

            if (activeCrystalData.shouldDecayFromTimer()) {
                OverVaults.LOGGER.info(
                        "Timed decay threshold reached for OverVault crystal at {} in {}. activeTicks={}, decayTimeSeconds={}. Attempting crystal removal.",
                        activeCrystalData.getPedestalPos(),
                        activeCrystalData.getDimension().location(),
                        activeCrystalData.getActiveTicks(),
                        activeCrystalData.getSecondsUntilDecay()
                );

                Component decayMessage = MiscUtil.getPortalMessage(activeCrystalData.getDecayTranslationComponent(), activeCrystalData.getDimension());
                if (OverVaultCrystalUtil.clearActiveCrystal(server, activeCrystalData)) {
                    activePortalTickCounter = 0;
                    actlRemoveModifierTimer = -1;

                    if (VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.BROADCAST_IN_CHAT) {
                        MiscUtil.broadcast(decayMessage);
                    }
                }
            }
        }

        if (portalSavedData.hasActiveOverVault()) {
            activePortalTickCounter++;
            PortalData activePortalData = portalSavedData.getFirstActivePortalData();
            if (activePortalData != null) {
                activePortalData.addActiveTick();
                portalSavedData.setDirty();

                if (activePortalData.getActiveVaultId() == null) {
                    PortalUtil.getActivePortalVaultId(server, activePortalData).ifPresent(vaultId -> {
                        activePortalData.setActiveVaultId(vaultId);
                        portalSavedData.setDirty();
                        OverVaults.LOGGER.info(
                                "Linked active OverVault portal at {} in {} to vault {}.",
                                activePortalData.getPortalFrameCenterPos(),
                                activePortalData.getDimension().location(),
                                vaultId
                        );
                    });
                }

                if (VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.UPDATE_VAULT_COMPASS && activePortalData.getActiveTicks() % 20 == 0) {
                    MiscUtil.sendCompassInfo(server, activePortalData.getDimension(), activePortalData.getPortalFrameCenterPos());
                }

                if (activePortalData.getActiveTicks() % 20 == 0 && shouldDeactivateEndedPortal(server, activePortalData)) {
                    PortalUtil.deactivatePortal(
                            server,
                            activePortalData,
                            true,
                            null
                    );
                    activePortalTickCounter = 0;
                    actlRemoveModifierTimer = -1;
                    return;
                }

                if (activePortalData.shouldDecayFromTimer()) {
                    OverVaults.LOGGER.info(
                            "Timed decay threshold reached for OverVault portal at {} in {}. activeTicks={}, decayTimeSeconds={}. Attempting portal shutdown.",
                            activePortalData.getPortalFrameCenterPos(),
                            activePortalData.getDimension().location(),
                            activePortalData.getActiveTicks(),
                            activePortalData.getSecondsUntilDecay()
                    );
                    PortalUtil.deactivatePortal(
                            server,
                            activePortalData,
                            true,
                            MiscUtil.getPortalMessage(activePortalData.getDecayTranslationComponent(), activePortalData.getDimension())
                    );
                    activePortalTickCounter = 0;
                    actlRemoveModifierTimer = -1;
                    return;
                }
            }

            if(actlRemoveModifierTimer == -1) actlRemoveModifierTimer = getRandomRemoveModifierTimer();
            if (shouldModifyPortal(actlRemoveModifierTimer)) {
                PortalData portalData = portalSavedData.getFirstActivePortalData();
                if (portalData == null) {
                    activePortalTickCounter = 0;
                    actlRemoveModifierTimer = -1;
                    return;
                }

                ServerLevel portalLevel = server.getLevel(portalData.getDimension());
                BlockEntityChunkSavedData entityChunkData = BlockEntityChunkSavedData.get(level);
                List<BlockPos> portalTilePositions = getTrackedPortalTilePositions(entityChunkData, portalData);
                boolean hasModified = false;

                if(portalLevel == null) {
                    OverVaults.LOGGER.error("OverVault Portal Level is null!");
                    return;
                }

                List<VaultModifierStack> sharedModifierList = null;

                for (int i = 0; i < portalTilePositions.size(); i++) {
                    BlockPos pos = portalTilePositions.get(i);

                    if (!portalLevel.hasChunkAt(pos)) {
                        OverVaults.LOGGER.warn("Chunk containing position {} is not loaded, attempting to load...", pos);
                        portalLevel.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, true); // Load the chunk if not loaded
                    }

                    if (!portalLevel.isLoaded(pos)) {
                        OverVaults.LOGGER.warn("Position {} is not loaded even after ensuring chunk load, skipping...", pos);
                        continue;
                    }

                    BlockState state = portalLevel.getBlockState(pos);

                    if(!state.is(ModBlocks.VAULT_PORTAL)) {
                        OverVaults.LOGGER.warn(
                                "Tracked active portal tile at {} is no longer a vault portal block (found {}). Rebuilding portal tile tracking.",
                                pos,
                                state.getBlock()
                        );
                        portalTilePositions = getTrackedPortalTilePositions(entityChunkData, portalData);
                        i = -1;

                        if (portalTilePositions.isEmpty()) {
                            OverVaults.LOGGER.error("Active OverVault portal at {} in {} no longer has any tracked portal tiles. Removing active state.",
                                    portalData.getPortalFrameCenterPos(),
                                    portalData.getDimension().location());
                            clearInvalidActivePortal(server, portalSavedData, entityChunkData, portalData);
                            return;
                        }

                        continue;
                    }

                    VaultPortalTileEntity portalTileEntity = (VaultPortalTileEntity) portalLevel.getBlockEntity(pos);
                    if(portalTileEntity == null || portalTileEntity.getData().isEmpty()) {
                        OverVaults.LOGGER.warn("Portal BlockEntity doesnt exist or data is empty, skipping");
                        continue;
                    }

                    List<VaultModifierStack> modifierList = portalTileEntity.getData().get().getModifiers().getList();

                    if (i == 0) {
                        // First portal tile entity: shuffle and modify the list
                        if (!modifierList.isEmpty()) {
                            Collections.shuffle(modifierList);
                            Iterator<VaultModifierStack> it = modifierList.iterator();

                            while (it.hasNext()) {
                                VaultModifierStack modifier = it.next();

                                if(!VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.BLACKLISTED_MODIFIERS_TO_REMOVE.contains(modifier.getModifierId().toString())) {
                                    if (modifier.shrink(1).isEmpty()) {
                                        it.remove();
                                    }

                                    sharedModifierList = new ArrayList<>(modifierList); // Clone the modified list
                                    hasModified = true;

                                    if (portalSavedData.getFirstActivePortalData().getModifiersRemoved() == -1) {
                                        portalSavedData.getFirstActivePortalData().setModifiersRemoved(0);
                                    } else {
                                        portalSavedData.getFirstActivePortalData().addModifiersRemoved(1);
                                    }
                                    break;
                                }
                            }
                        }
                    } else if (sharedModifierList != null) {
                        // Subsequent portal tile entities: apply the shared list
                        modifierList.clear();
                        modifierList.addAll(sharedModifierList);
                    }
                }

                if(hasModified && VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.SPAWN_ENTITY_MODIFIER_REMOVAL) {
                    int removed = portalData.getModifiersRemoved();
                    int step = removed / VaultConfigRegistry.OVERVAULTS_MOB_CONFIG.MODIFIERS_TO_REMOVE_UNTIL_TIER_UP; // Technical step, ignoring max cap, for bosses
                    VaultConfigRegistry.OVERVAULTS_MOB_CONFIG.addPortalEntityToWorld(portalLevel, step, portalData);

                    if(step == 5 && entityChunkData.isMarkedForRemoval()) {
                        PortalUtil.deactivatePortal(
                                server,
                                portalData,
                                true,
                                MiscUtil.getPortalMessage(portalData.getDecayTranslationComponent(), portalData.getDimension())
                        );
                    }
                }

                activePortalTickCounter = 0; // Reset the counter after execution
                actlRemoveModifierTimer = getRandomRemoveModifierTimer(); //reset the random counter thingy yadda yadda
            }
        }
    }


    /**
     * Helper method to determine if a portal should spawn
     * @return true/false whether the portal should spawn
     */
    private static boolean shouldSpawnPortal(int ticksForPortalSpawn) {
        return counter >= ticksForPortalSpawn;
    }

    /**
     * Helper method to determine if a portal should be modified
     * @return true/false whether the portal should be modified
     */
    private static boolean shouldModifyPortal(int removeModifierTimer) {
        return activePortalTickCounter >= removeModifierTimer;
    }

    /**
     * Returns a random value for the ticks until portal spawn.
     */
    private static int getRandomTicksForPortalSpawn() {
        return VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.SECONDS_UNTIL_PORTAL_SPAWN.getRandom() * 20;
    }

    /**
     * Returns a random value for the ticks until modifier removal.
     */
    private static int getRandomRemoveModifierTimer() {
        return VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.SECONDS_UNTIL_MODIFIER_REMOVAL.getRandom() * 20;
    }

    private static List<BlockPos> getTrackedPortalTilePositions(BlockEntityChunkSavedData entityChunkData, PortalData portalData) {
        List<BlockPos> trackedPositions = entityChunkData.getPortalTilePositions();
        if (!trackedPositions.isEmpty()) {
            return trackedPositions;
        }

        trackedPositions.addAll(getExpectedPortalTilePositions(portalData));
        entityChunkData.setDirty();
        return trackedPositions;
    }

    private static List<BlockPos> getExpectedPortalTilePositions(PortalData portalData) {
        List<BlockPos> expectedPositions = new ArrayList<>();
        portalData.getSize()
                .getBlockPositions(portalData.getPortalFrameCenterPos(), portalData.getRotation())
                .forEach(expectedPositions::add);
        return expectedPositions;
    }

    private static boolean shouldDeactivateEndedPortal(MinecraftServer server, PortalData portalData) {
        UUID activeVaultId = portalData.getActiveVaultId();
        if (activeVaultId != null && ServerVaults.get(activeVaultId).isEmpty()) {
            OverVaults.LOGGER.info(
                    "Active OverVault portal at {} in {} is linked to ended vault {}. Deactivating portal.",
                    portalData.getPortalFrameCenterPos(),
                    portalData.getDimension().location(),
                    activeVaultId
            );
            return true;
        }

        if (!PortalUtil.activePortalHasPortalBlocks(server, portalData)) {
            OverVaults.LOGGER.info(
                    "Active OverVault portal at {} in {} no longer has vault portal blocks. Deactivating portal.",
                    portalData.getPortalFrameCenterPos(),
                    portalData.getDimension().location()
            );
            return true;
        }

        return false;
    }

    private static void clearInvalidActivePortal(MinecraftServer server, PortalSavedData portalSavedData, BlockEntityChunkSavedData entityChunkData, PortalData portalData) {
        entityChunkData.removePortalTileEntityData();

        for (ChunkPos chunkPos : entityChunkData.getForceloadedChunks()) {
            ServerLevel overworld = server.overworld();
            overworld.setChunkForced(chunkPos.x, chunkPos.z, false);
            overworld.getChunkSource().removeRegionTicket(OverVaultConstants.OVERVAULT_TICKET, chunkPos, 2, chunkPos);
        }

        entityChunkData.removeForceLoadedChunkData();
        entityChunkData.setMarkedForRemoval(false);
        portalData.setActiveState(false);
        portalData.setModifiersRemoved(-1);
        portalSavedData.setDirty();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.getLevel().dimension().location().getNamespace().equals("the_vault")) {
                MiscUtil.clearCompassInfoForPlayer(player);
            }
        }
    }


}
