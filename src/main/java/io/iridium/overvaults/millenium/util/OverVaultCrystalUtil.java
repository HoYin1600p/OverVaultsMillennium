package io.iridium.overvaults.millenium.util;

import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.config.VaultConfigRegistry;
import io.iridium.overvaults.config.vault.entry.PortalEntry;
import io.iridium.overvaults.millenium.world.ActiveCrystalSavedData;
import io.iridium.overvaults.millenium.world.PortalData;
import io.iridium.overvaults.millenium.world.StructureSize;
import iskallia.vault.core.vault.modifier.VaultModifierStack;
import iskallia.vault.core.vault.modifier.registry.VaultModifierRegistry;
import iskallia.vault.init.ModItems;
import iskallia.vault.item.crystal.CrystalData;
import iskallia.vault.world.data.PlayerVaultStatsData;
import net.mehvahdjukaar.selene.blocks.ItemDisplayTile;
import net.mehvahdjukaar.supplementaries.common.block.blocks.PedestalBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class OverVaultCrystalUtil {
    public static final String OVERVAULT_CRYSTAL_TAG = "OverVaultCrystal";
    public static final int MAX_OVERVAULT_LEVEL = 100;

    private static final int CRYSTAL_PEDESTAL_LOOKUP_Y_SEARCH_RANGE = 8;
    private static final int CRYSTAL_PEDESTAL_MAX_GROUND_DROP = 3;
    private static final ResourceLocation PEDESTAL_ID = new ResourceLocation("supplementaries", "pedestal");
    private static final ResourceLocation BEGINNERS_INSURANCE_ID = new ResourceLocation("the_vault", "beginners_insurance");
    private static final ResourceLocation BEGINNERS_GRACE_ID = new ResourceLocation("the_vault", "beginners_grace");

    public static ItemStack createCrystalStack(CrystalData crystalData) {
        ItemStack stack = new ItemStack(ModItems.VAULT_CRYSTAL);
        CrystalData overVaultCrystalData = crystalData.copy();
        overVaultCrystalData.getProperties().setUnmodifiable(true);
        overVaultCrystalData.write(stack);
        stack.getOrCreateTag().putBoolean(OVERVAULT_CRYSTAL_TAG, true);
        return stack;
    }

    public static boolean isOverVaultCrystal(ItemStack stack) {
        return !stack.isEmpty()
                && stack.is(ModItems.VAULT_CRYSTAL)
                && stack.hasTag()
                && stack.getTag().getBoolean(OVERVAULT_CRYSTAL_TAG);
    }

    public static void setCrystalLevelFromCollector(ItemStack stack, ServerPlayer player) {
        if (!isOverVaultCrystal(stack)) {
            return;
        }

        CrystalData crystalData = CrystalData.read(stack);
        int playerVaultLevel = capOverVaultLevel(PlayerVaultStatsData.get((ServerLevel) player.level).getVaultStats(player).getVaultLevel());

        if (playerVaultLevel <= 20) {
            addBeginnerModifierIfMissing(crystalData, BEGINNERS_INSURANCE_ID);
            addBeginnerModifierIfMissing(crystalData, BEGINNERS_GRACE_ID);
        }

        if (VaultConfigRegistry.OVERVAULTS_GENERAL_CONFIG.SET_LEVEL_OF_ENTERING_PLAYER_OVERVAULT) {
            crystalData.getProperties().setLevel(playerVaultLevel);
        } else if (crystalData.getProperties().getLevel().isEmpty()) {
            crystalData.getProperties().setLevel(0);
        }

        crystalData.write(stack);
        stack.getOrCreateTag().putBoolean(OVERVAULT_CRYSTAL_TAG, true);
    }

    public static int capOverVaultLevel(int level) {
        return Math.min(level, MAX_OVERVAULT_LEVEL);
    }

    public static Optional<BlockPos> placeCrystalPedestal(ServerLevel level, PortalData data, ItemStack crystalStack) {
        Block pedestalBlock = ForgeRegistries.BLOCKS.getValue(PEDESTAL_ID);
        if (!(pedestalBlock instanceof PedestalBlock)) {
            OverVaults.LOGGER.error("Could not place OverVault crystal because '{}' is not registered as a Supplementaries pedestal.", PEDESTAL_ID);
            return Optional.empty();
        }

        BlockPos pedestalPos = getPedestalPos(level, data);
        ChunkPos chunkPos = new ChunkPos(pedestalPos);
        level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, true);

        Direction.Axis axis = getPedestalAxis(data.getRotation());
        BlockState pedestalState = pedestalBlock.defaultBlockState()
                .setValue(PedestalBlock.HAS_ITEM, true)
                .setValue(PedestalBlock.AXIS, axis);

        level.setBlock(pedestalPos, pedestalState, Block.UPDATE_ALL);
        BlockEntity blockEntity = level.getBlockEntity(pedestalPos);
        if (blockEntity instanceof ItemDisplayTile displayTile) {
            displayTile.setDisplayedItem(crystalStack.copy());
            return Optional.of(pedestalPos);
        }

        OverVaults.LOGGER.error("Placed OverVault crystal pedestal at {}, but no display tile entity was created.", pedestalPos);
        return Optional.empty();
    }

    public static BlockPos getPedestalPos(ServerLevel level, PortalData data) {
        return findPedestalPos(level, data);
    }

    public static boolean clearActiveCrystal(MinecraftServer server, ActiveCrystalSavedData activeCrystalData) {
        ServerLevel level = server.getLevel(activeCrystalData.getDimension());
        if (level == null) {
            OverVaults.LOGGER.error("Attempted to clear an OverVault crystal, but the Level of the crystal equals null.");
            return false;
        }

        BlockPos pedestalPos = getActiveCrystalPedestalPos(server, activeCrystalData).orElse(activeCrystalData.getPedestalPos());
        ChunkPos chunkPos = new ChunkPos(pedestalPos);
        level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, true);

        if (!level.hasChunkAt(pedestalPos)) {
            OverVaults.LOGGER.warn("Could not clear OverVault crystal at {} because the chunk is not loaded.", pedestalPos);
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pedestalPos);
        if (blockEntity instanceof ItemDisplayTile displayTile) {
            ItemStack displayed = displayTile.getDisplayedItem();
            if (isOverVaultCrystal(displayed)) {
                displayTile.setDisplayedItem(ItemStack.EMPTY);
            }
        }

        BlockState state = level.getBlockState(pedestalPos);
        if (state.getBlock() instanceof PedestalBlock && state.hasProperty(PedestalBlock.HAS_ITEM)) {
            level.setBlock(pedestalPos, state.setValue(PedestalBlock.HAS_ITEM, false), Block.UPDATE_ALL);
        }

        activeCrystalData.clearActiveCrystal();
        clearCompassInfo(server);
        return true;
    }

    public static boolean activeCrystalStillExists(MinecraftServer server, ActiveCrystalSavedData activeCrystalData) {
        ServerLevel level = server.getLevel(activeCrystalData.getDimension());
        if (level == null) {
            return false;
        }

        BlockPos pedestalPos = activeCrystalData.getPedestalPos();
        if (!level.hasChunkAt(pedestalPos)) {
            return true;
        }

        Optional<BlockPos> actualPedestalPos = findActiveCrystalPedestalPos(level, pedestalPos);
        if (actualPedestalPos.isEmpty()) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(actualPedestalPos.get());
        if (!(blockEntity instanceof ItemDisplayTile displayTile)) {
            return false;
        }

        return isOverVaultCrystal(displayTile.getDisplayedItem());
    }

    public static Optional<BlockPos> getActiveCrystalPedestalPos(MinecraftServer server, ActiveCrystalSavedData activeCrystalData) {
        ServerLevel level = server.getLevel(activeCrystalData.getDimension());
        if (level == null) {
            return Optional.empty();
        }

        BlockPos expectedPos = activeCrystalData.getPedestalPos();
        if (!level.hasChunkAt(expectedPos)) {
            return Optional.of(expectedPos);
        }

        return findActiveCrystalPedestalPos(level, expectedPos);
    }

    public static Optional<BlockPos> findActiveCrystalPedestalPos(ServerLevel level, BlockPos expectedPos) {
        for (int offset = 0; offset <= CRYSTAL_PEDESTAL_LOOKUP_Y_SEARCH_RANGE; offset++) {
            BlockPos lower = expectedPos.below(offset);
            if (hasOverVaultCrystal(level, lower)) {
                return Optional.of(lower);
            }

            if (offset > 0) {
                BlockPos higher = expectedPos.above(offset);
                if (hasOverVaultCrystal(level, higher)) {
                    return Optional.of(higher);
                }
            }
        }

        return Optional.empty();
    }

    public static void giveCrystalToCollector(ServerPlayer player, InteractionHand hand, ItemStack crystalStack) {
        ItemStack toGive = crystalStack.copy();

        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, toGive);
            return;
        }

        if (player.getInventory().add(toGive)) {
            return;
        }

        player.drop(toGive, false);
    }

    public static void broadcastCrystalClaim(ServerPlayer player) {
        broadcastCrystalClaim(player, "OverVault Crystal");
    }

    public static void broadcastCrystalClaim(ServerPlayer player, String crystalTitle) {
        MutableComponent message = new TextComponent("")
                .append(new TextComponent(player.getName().getString()).withStyle(ChatFormatting.GOLD))
                .append(new TextComponent(" has accepted the OverVault challenge and claimed it's ").withStyle(ChatFormatting.AQUA))
                .append(new TextComponent(crystalTitle == null || crystalTitle.isBlank() ? "OverVault Crystal" : crystalTitle).withStyle(ChatFormatting.GOLD))
                .append(new TextComponent(".").withStyle(ChatFormatting.AQUA));
        MiscUtil.broadcast(message);
    }

    public static String getClaimCrystalTitle(PortalEntry portalEntry) {
        if (portalEntry == null) {
            return "OverVault Crystal";
        }

        String rank = getRankFromPortalOpenLang(portalEntry.getPortalOpenLang());
        return rank == null ? "OverVault Crystal" : rank + "-Rank Crystal";
    }

    public static void clearCompassInfo(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.getLevel().dimension().location().getNamespace().equals("the_vault")) {
                MiscUtil.clearCompassInfoForPlayer(player);
            }
        }
    }

    private static void addBeginnerModifierIfMissing(CrystalData crystalData, ResourceLocation modifierId) {
        if (crystalData.getModifiers().getList().stream().anyMatch(modifier -> modifierId.equals(modifier.getModifierId()))) {
            return;
        }

        VaultModifierRegistry.getOpt(modifierId).ifPresentOrElse(
                modifier -> crystalData.getModifiers().getList().add(VaultModifierStack.of(modifier, 1)),
                () -> OverVaults.LOGGER.warn("Failed to add beginner OverVault modifier '{}' because it is not registered.", modifierId)
        );
    }

    private static String getRankFromPortalOpenLang(String portalOpenLang) {
        if (portalOpenLang == null || portalOpenLang.isBlank()) {
            return null;
        }

        String rank = getRankFromParenthetical(portalOpenLang);
        if (rank != null) {
            return rank;
        }

        return getRankFromTranslationKey(portalOpenLang);
    }

    private static String getRankFromParenthetical(String text) {
        int close = text.lastIndexOf(')');
        int open = close < 0 ? -1 : text.lastIndexOf('(', close);
        if (open < 0 || close <= open) {
            return null;
        }

        String title = stripFormatting(text.substring(open + 1, close)).trim();
        int tierIndex = title.toLowerCase(Locale.ROOT).indexOf("-tier");
        if (tierIndex < 0) {
            tierIndex = title.toLowerCase(Locale.ROOT).indexOf(" tier");
        }

        if (tierIndex > 0) {
            return title.substring(0, tierIndex).trim();
        }

        int rankIndex = title.toLowerCase(Locale.ROOT).indexOf("-rank");
        if (rankIndex < 0) {
            rankIndex = title.toLowerCase(Locale.ROOT).indexOf(" rank");
        }

        if (rankIndex > 0) {
            return title.substring(0, rankIndex).trim();
        }

        return null;
    }

    private static String getRankFromTranslationKey(String key) {
        String suffix = key.substring(key.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (suffix) {
            case "splusplus" -> "S++";
            case "splus" -> "S+";
            case "s" -> "S";
            case "a" -> "A";
            case "b" -> "B";
            case "c" -> "C";
            case "d" -> "D";
            case "e" -> "E";
            default -> null;
        };
    }

    private static String stripFormatting(String text) {
        StringBuilder stripped = new StringBuilder();
        boolean skipNext = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (skipNext) {
                skipNext = false;
                continue;
            }

            if (c == '\u00A7') {
                skipNext = true;
                continue;
            }

            stripped.append(c);
        }

        return stripped.toString();
    }

    private static BlockPos findPedestalPos(ServerLevel level, PortalData data) {
        Direction front = getFrontDirection(data.getRotation());
        int distance = data.getSize() == StructureSize.SMALL ? 4 : 5;
        int baseY = StreamSupport.stream(data.getSize().getFrameBlockPositions(data.getPortalFrameCenterPos(), data.getRotation()).spliterator(), false)
                .min(Comparator.comparingInt(BlockPos::getY))
                .map(BlockPos::getY)
                .orElse(data.getPortalFrameCenterPos().getY());

        BlockPos basePos = data.getPortalFrameCenterPos()
                .relative(front, distance);
        basePos = new BlockPos(basePos.getX(), baseY, basePos.getZ());

        Optional<BlockPos> grounded = findNearbyGroundedPosition(level, basePos);
        return grounded.orElse(basePos);
    }

    private static Optional<BlockPos> findNearbyGroundedPosition(ServerLevel level, BlockPos basePos) {
        for (int offset = 0; offset <= CRYSTAL_PEDESTAL_LOOKUP_Y_SEARCH_RANGE; offset++) {
            if (offset <= CRYSTAL_PEDESTAL_MAX_GROUND_DROP) {
                BlockPos lower = basePos.below(offset);
                if (hasGround(level, lower)) {
                    return Optional.of(lower);
                }
            }

            if (offset > 0) {
                BlockPos higher = basePos.above(offset);
                if (hasGround(level, higher)) {
                    return Optional.of(higher);
                }
            }
        }

        return Optional.empty();
    }

    private static boolean hasGround(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos.below()).getMaterial().isReplaceable();
    }

    private static boolean hasOverVaultCrystal(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof ItemDisplayTile displayTile
                && isOverVaultCrystal(displayTile.getDisplayedItem());
    }

    private static Direction getFrontDirection(Rotation rotation) {
        return switch (rotation) {
            case NONE -> Direction.SOUTH;
            case CLOCKWISE_90 -> Direction.WEST;
            case CLOCKWISE_180 -> Direction.NORTH;
            case COUNTERCLOCKWISE_90 -> Direction.EAST;
        };
    }

    private static Direction.Axis getPedestalAxis(Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> Direction.Axis.Z;
            case NONE, CLOCKWISE_180 -> Direction.Axis.X;
        };
    }
}
