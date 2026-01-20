package io.iridium.overvaults.millenium.util;

import com.mojang.datafixers.util.Pair;
import io.iridium.overvaults.OverVaultConstants;
import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.config.vault.OverVaultsPortalConfig;
import io.iridium.overvaults.config.vault.entry.PortalEntry;
import io.iridium.overvaults.millenium.world.BlockEntityChunkSavedData;
import io.iridium.overvaults.millenium.world.PortalData;
import io.iridium.overvaults.millenium.world.PortalSavedData;
import io.iridium.overvaults.millenium.world.StructureSize;
import iskallia.vault.block.entity.VaultPortalTileEntity;
import iskallia.vault.init.ModBlocks;
import iskallia.vault.init.ModConfigs;
import iskallia.vault.item.crystal.CrystalData;
import net.minecraft.core.BlockPos;
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
        Block[] b = ModConfigs.VAULT_PORTAL.getValidFrameBlocks();
        List<Block> a = new ArrayList<>(Arrays.stream(b.clone()).toList());
        a.remove(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("the_vault:bumbo_polished_vault_stone")));
        a.remove(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("the_vault:final_vault_frame")));
        a.remove(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("the_vault:vault_stone_pillar")));
        a.remove(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("the_vault:vault_cobblestone")));

        for(BlockPos framePos : framePosList) {
            Block toPlace = a.get(rand.nextInt(a.size()));
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
        entityChunkData.setMarkedForRemoval(pairEntry.getFirst().shouldPortalDecay());

        entityChunkData.setDirty();
        portalSavedData.setDirty();
        MiscUtil.notifyPlayers(server, data, pairEntry.getFirst().getTranslationComponent());
        return true;
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
