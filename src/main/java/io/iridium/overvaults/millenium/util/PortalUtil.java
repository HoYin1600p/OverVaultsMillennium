package io.iridium.overvaults.millenium.util;

import com.mojang.datafixers.util.Pair;
import io.iridium.overvaults.OverVaultConstants;
import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.config.VaultConfigRegistry;
import io.iridium.overvaults.config.vault.OverVaultsPortalConfig;
import io.iridium.overvaults.config.vault.entry.PortalEntry;
import io.iridium.overvaults.millenium.world.BlockEntityChunkSavedData;
import io.iridium.overvaults.millenium.world.ActiveCrystalSavedData;
import io.iridium.overvaults.millenium.world.OverVaultsPerformanceSavedData;
import io.iridium.overvaults.millenium.world.PortalData;
import io.iridium.overvaults.millenium.world.PortalSavedData;
import io.iridium.overvaults.millenium.world.StructureSize;
import iskallia.vault.block.entity.VaultPortalTileEntity;
import iskallia.vault.init.ModBlocks;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.item.crystal.CrystalData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class PortalUtil {
    private static final Random rand = new Random();


    public static PortalData getRandomPortalData(List<PortalData> portalDataList) {
        return portalDataList.get(rand.nextInt(portalDataList.size()));
    }

    /**
     * Activates an OverVault Portal
     *
     * @param level Level used to force-load chunks, fill blocks, get block entities & determine the Crystal Data of the Portal
     * @param data The Portal data which we would like to activate, used to determine size, position & rotation of the Portal
     * @param crystalData The CrystalData used for the Portal
     * @return A List of Vault Portal tile entities that have been filled
     */
    public static List<VaultPortalTileEntity> portalTileActivation(ServerLevel level, PortalData data, CrystalData crystalData) {
        List<VaultPortalTileEntity> vaultPortalTileEntities = new ArrayList<>();
        BlockEntityChunkSavedData entityChunkData = BlockEntityChunkSavedData.get(level);
        Iterable<BlockPos> blocksToFill = data.getSize().getBlockPositions(data.getPortalFrameCenterPos(), data.getRotation());

        for(BlockPos pos : blocksToFill) {
            ChunkPos chunkPos = new ChunkPos(pos);

            if(!entityChunkData.getForceloadedChunks().contains(chunkPos)) {
                level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true);
                entityChunkData.addForceloadedChunk(chunkPos.x, chunkPos.z);
                level.setChunkForced(chunkPos.x, chunkPos.z, true);
                level.getChunkSource().addRegionTicket(OverVaultConstants.OVERVAULT_TICKET, chunkPos, 2, chunkPos);
            }
        }



        blocksToFill.forEach(pos -> {
            level.setBlock(pos, ModBlocks.VAULT_PORTAL.defaultBlockState().rotate(data.getRotation()), 3);
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof VaultPortalTileEntity portalTE) {
                CrystalData crystalDataCopy = crystalData.copy();
                portalTE.setCrystalData(crystalDataCopy);
                vaultPortalTileEntities.add(portalTE);
            }
        });

        return vaultPortalTileEntities;
    }

    /**
     * Helper method to get the Closest Portal to the player.
     *
     * @param player the Player whose distance is being checked.
     * @return a Pair containing the PortalData of the closest Portal & the distance between the Portal and the Player.
     */
    public static Pair<PortalData, Double> getClosestPortalData(ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();

        // Get the filtered list of portals in the player's level
        List<PortalData> filteredPortalList = filteredPortalList(player.getLevel());

        PortalData closestPortal = null;
        double closestDistance = Double.MAX_VALUE;

        // Iterate through the portal list to find the closest one
        for (PortalData data : filteredPortalList) {
            double distance = data.getPortalFrameCenterPos().distSqr(playerPos);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestPortal = data;
            }
        }

        // Return the closest PortalData and the square root of the distance (Euclidean distance)
        return closestPortal == null ? null : new Pair<>(closestPortal, Math.sqrt(closestDistance));
    }

    /**
     * Helper method to get all {@code PortalData} objects in the given Level
     *
     * @param level The {@code Level} to filter for
     * @return List of all the Portals in the given Level
     */
    public static List<PortalData> filteredPortalList(Level level) {
        return PortalSavedData.getServer().getPortalData()
                .stream()
                .filter(p -> p.getDimension().equals(level.dimension()))
                .toList();
    }

    public static boolean activatePortal(MinecraftServer server, PortalData data) {
        ServerLevel portalLevel = server.getLevel(data.getDimension());
        if(portalLevel == null) {
            OverVaults.LOGGER.error("Attempted to activate an Overvault portal, but the Level of the portal equals null.");
            return false;
        }

        boolean valid = PortalUtil.hasValidFrameBlocks(portalLevel, data);
        if(!valid) {
            OverVaults.LOGGER.error("Attempted to activate Overvault portal, but the given portal had an invalid frame.");
            return false;
        }

        List<BlockPos> framePosList = data.getSize().getFrameBlockPositions(data.getPortalFrameCenterPos(), data.getRotation());
        List<Block> frameBlocks = new ArrayList<>();
        frameBlocks.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("the_vault:vault_stone_bricks")));
        frameBlocks.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("the_vault:vault_stone_bricks_cracked")));
        frameBlocks.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("the_vault:vault_stone")));
        frameBlocks.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("the_vault:vault_cobblestone")));

        for(BlockPos framePos : framePosList) {
            Block toPlace = frameBlocks.get(rand.nextInt(frameBlocks.size()));
            portalLevel.setBlock(framePos, toPlace.defaultBlockState(), Block.UPDATE_ALL);
        }

        Pair<PortalEntry, CrystalData> pairEntry = OverVaultsPortalConfig.getRandomCrystalData(data.getDimension());
        if (pairEntry.getFirst() == null) {
            OverVaults.LOGGER.error("Failed to get PortalEntry from config - PORTAL_LIST may be empty or misconfigured.");
            return false;
        }

        BlockEntityChunkSavedData entityChunkData = BlockEntityChunkSavedData.getServer();
        PortalSavedData portalSavedData = PortalSavedData.getServer();
        List<VaultPortalTileEntity> portalTileEntities = PortalUtil.portalTileActivation(portalLevel, data, pairEntry.getSecond());

        for (VaultPortalTileEntity portalTileEntity : portalTileEntities) {
            entityChunkData.addPortalTileEntity(portalTileEntity.getBlockPos());
        }

        data.setActiveState(true);
        data.setActivePortalConfig(
                pairEntry.getFirst().shouldPortalDecay() ? pairEntry.getFirst().getDecayTime() : -1,
                pairEntry.getFirst().getLoginMessage(),
                pairEntry.getFirst().getPortalDecayed()
        );
        entityChunkData.setMarkedForRemoval(pairEntry.getFirst().shouldPortalDecay());

        OverVaults.LOGGER.info(
                "Activated OverVault portal at {} in {}. shouldDecay={}, decayTimeSeconds={}, portalTileCount={}",
                data.getPortalFrameCenterPos(),
                data.getDimension().location(),
                pairEntry.getFirst().shouldPortalDecay(),
                data.getSecondsUntilDecay(),
                portalTileEntities.size()
        );

        entityChunkData.setDirty();
        portalSavedData.setDirty();
        MiscUtil.notifyPlayers(server, data, pairEntry.getFirst().getPortalOpenLang());
        return true;
    }

    public static boolean activateOverVault(MinecraftServer server, PortalData data) {
        if (OverVaultsPerformanceSavedData.get(server).getMode() == OverVaultsPerformanceSavedData.Mode.CRYSTAL) {
            return activateCrystal(server, data);
        }

        return activatePortal(server, data);
    }

    public static boolean activateCrystal(MinecraftServer server, PortalData data) {
        ServerLevel portalLevel = server.getLevel(data.getDimension());
        if(portalLevel == null) {
            OverVaults.LOGGER.error("Attempted to activate an OverVault crystal, but the Level of the portal equals null.");
            return false;
        }

        if (ActiveCrystalSavedData.get(server).hasActiveCrystal()) {
            OverVaults.LOGGER.warn("Attempted to activate an OverVault crystal while another crystal is already active.");
            return false;
        }

        boolean valid = PortalUtil.hasValidFrameBlocks(portalLevel, data);
        if(!valid) {
            OverVaults.LOGGER.error("Attempted to activate OverVault crystal, but the given portal had an invalid frame.");
            return false;
        }

        Pair<PortalEntry, CrystalData> pairEntry = OverVaultsPortalConfig.getRandomCrystalData(data.getDimension());
        if (pairEntry.getFirst() == null) {
            OverVaults.LOGGER.error("Failed to get PortalEntry from config - PORTAL_LIST may be empty or misconfigured.");
            return false;
        }

        Optional<BlockPos> pedestalPos = OverVaultCrystalUtil.placeCrystalPedestal(portalLevel, data, OverVaultCrystalUtil.createCrystalStack(pairEntry.getSecond()));
        if (pedestalPos.isEmpty()) {
            return false;
        }

        PortalSavedData portalSavedData = PortalSavedData.getServer();
        ActiveCrystalSavedData activeCrystalData = ActiveCrystalSavedData.get(server);
        activeCrystalData.setActiveCrystal(
                pedestalPos.get(),
                data.getDimension(),
                portalSavedData.getPortalData().indexOf(data),
                pairEntry.getFirst().shouldPortalDecay() ? pairEntry.getFirst().getDecayTime() : -1,
                pairEntry.getFirst().getPortalDecayed(),
                OverVaultCrystalUtil.getClaimCrystalTitle(pairEntry.getFirst())
        );

        OverVaults.LOGGER.info(
                "Activated OverVault crystal at {} in {}. shouldDecay={}, decayTimeSeconds={}",
                activeCrystalData.getPedestalPos(),
                data.getDimension().location(),
                pairEntry.getFirst().shouldPortalDecay(),
                activeCrystalData.getSecondsUntilDecay()
        );

        MiscUtil.notifyPlayers(server, data, pairEntry.getFirst().getPortalOpenLang(), activeCrystalData.getPedestalPos());
        return true;
    }

    public static void deactivatePortal(MinecraftServer server, PortalData data, boolean removePortalBlocks, Component broadcastMessage) {
        ServerLevel portalLevel = server.getLevel(data.getDimension());
        if (portalLevel == null) {
            OverVaults.LOGGER.error("Attempted to deactivate an OverVault portal, but the Level of the portal equals null.");
            return;
        }

        BlockEntityChunkSavedData entityChunkData = BlockEntityChunkSavedData.get(server);
        PortalSavedData portalSavedData = PortalSavedData.get(server);

        OverVaults.LOGGER.info(
                "Attempting to deactivate OverVault portal at {} in {}. removePortalBlocks={}, portalTileCount={}, forcedChunkCount={}, activeTicks={}, secondsUntilDecay={}",
                data.getPortalFrameCenterPos(),
                data.getDimension().location(),
                removePortalBlocks,
                entityChunkData.getPortalTilePositions().size(),
                entityChunkData.getForceloadedChunks().size(),
                data.getActiveTicks(),
                data.getSecondsUntilDecay()
        );

        if (removePortalBlocks) {
            for (BlockPos pos : entityChunkData.getPortalTilePositions()) {
                if (portalLevel.isLoaded(pos)) {
                    portalLevel.removeBlock(pos, false);
                } else {
                    OverVaults.LOGGER.error("Position {} not loaded when deactivating portal!", pos);
                }
            }
        }

        entityChunkData.removePortalTileEntityData();
        entityChunkData.setMarkedForRemoval(false);
        data.setActiveState(false);
        data.setModifiersRemoved(-1);
        portalSavedData.setDirty();

        for (ChunkPos chunkPos : entityChunkData.getForceloadedChunks()) {
            portalLevel.setChunkForced(chunkPos.x, chunkPos.z, false);
            portalLevel.getChunkSource().removeRegionTicket(OverVaultConstants.OVERVAULT_TICKET, chunkPos, 2, chunkPos);
        }
        entityChunkData.removeForceLoadedChunkData();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.getLevel().dimension().location().getNamespace().equals("the_vault")) {
                MiscUtil.clearCompassInfoForPlayer(player);
            }
        }

        if (broadcastMessage != null && VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.BROADCAST_IN_CHAT) {
            MiscUtil.broadcast(broadcastMessage);
        }

        OverVaults.LOGGER.info(
                "Finished deactivating OverVault portal at {} in {}.",
                data.getPortalFrameCenterPos(),
                data.getDimension().location()
        );
    }

    public static Optional<UUID> getActivePortalVaultId(MinecraftServer server, PortalData data) {
        ServerLevel portalLevel = server.getLevel(data.getDimension());
        if (portalLevel == null) {
            return Optional.empty();
        }

        Set<BlockPos> portalTilePositions = new LinkedHashSet<>(BlockEntityChunkSavedData.get(server).getPortalTilePositions());
        data.getSize()
                .getBlockPositions(data.getPortalFrameCenterPos(), data.getRotation())
                .forEach(portalTilePositions::add);

        for (BlockPos pos : portalTilePositions) {
            if (!portalLevel.hasChunkAt(pos)) {
                portalLevel.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true);
            }

            if (!portalLevel.isLoaded(pos)) {
                continue;
            }

            BlockEntity blockEntity = portalLevel.getBlockEntity(pos);
            if (!(blockEntity instanceof VaultPortalTileEntity portalTileEntity)) {
                continue;
            }

            Optional<UUID> vaultId = portalTileEntity.getData()
                    .map(crystalData -> crystalData.getProperties().getVaultId());
            if (vaultId.isPresent()) {
                return vaultId;
            }
        }

        return Optional.empty();
    }

    public static boolean activePortalHasPortalBlocks(MinecraftServer server, PortalData data) {
        ServerLevel portalLevel = server.getLevel(data.getDimension());
        if (portalLevel == null) {
            return false;
        }

        Set<BlockPos> portalTilePositions = new LinkedHashSet<>(BlockEntityChunkSavedData.get(server).getPortalTilePositions());
        data.getSize()
                .getBlockPositions(data.getPortalFrameCenterPos(), data.getRotation())
                .forEach(portalTilePositions::add);

        for (BlockPos pos : portalTilePositions) {
            if (!portalLevel.hasChunkAt(pos)) {
                portalLevel.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true);
            }

            if (portalLevel.isLoaded(pos) && portalLevel.getBlockState(pos).is(ModBlocks.VAULT_PORTAL)) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasValidFrameBlocks(ServerLevel level, PortalData data) {


        // Positions to check around the portal center
        BlockPos[] positions;
        if (Objects.requireNonNull(data.getSize()) == StructureSize.SMALL) {
            switch (data.getRotation()) {
                case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> positions = new BlockPos[]{
                        data.getPortalFrameCenterPos().north(2),
                        data.getPortalFrameCenterPos().south(2)
                };
                case CLOCKWISE_180, NONE -> positions = new BlockPos[]{
                        data.getPortalFrameCenterPos().east(2),
                        data.getPortalFrameCenterPos().west(2)
                };
                default -> throw new IllegalArgumentException("Unsupported rotation: " + data.getRotation());
            }
        } else {
            switch (data.getRotation()) {
                case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> positions = new BlockPos[]{
                        data.getPortalFrameCenterPos().north(3),
                        data.getPortalFrameCenterPos().south(3)
                };
                case CLOCKWISE_180, NONE -> positions = new BlockPos[]{
                        data.getPortalFrameCenterPos().east(3),
                        data.getPortalFrameCenterPos().west(3)
                };
                default -> throw new IllegalArgumentException("Unsupported rotation: " + data.getRotation());
            }
        }

        int validCount = 0;
        Block[] validFrameBlocks = ModConfigs.VAULT_PORTAL.getValidFrameBlocks();

        for (BlockPos pos : positions) {
            BlockState blockState = level.getBlockState(pos);
            boolean isValid = false;

            // Check if the block matches any of the valid frame blocks
            for (Block validBlock : validFrameBlocks) {
                if (blockState.is(validBlock)) {
                    isValid = true;
                    validCount++;
                    break;
                }
            }

            if (!isValid) {
                OverVaults.LOGGER.warn("Expected valid frame block at {} but found {}", pos, blockState.getBlock());
            }
        }


        return validCount >= 2; // Return true if at least 2 valid blocks are found
    }
}
