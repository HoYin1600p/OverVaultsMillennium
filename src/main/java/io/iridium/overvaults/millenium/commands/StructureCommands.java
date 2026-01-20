package io.iridium.overvaults.millenium.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import io.iridium.overvaults.OverVaultConstants;
import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.config.VaultConfigRegistry;
import io.iridium.overvaults.config.vault.OverVaultsPortalConfig;
import io.iridium.overvaults.config.vault.entry.PortalEntry;
import io.iridium.overvaults.millenium.event.ServerTickEvent;
import io.iridium.overvaults.millenium.util.MiscUtil;
import io.iridium.overvaults.millenium.util.PortalUtil;
import io.iridium.overvaults.millenium.util.TextUtil;
import io.iridium.overvaults.millenium.world.BlockEntityChunkSavedData;
import io.iridium.overvaults.millenium.world.PortalData;
import io.iridium.overvaults.millenium.world.PortalSavedData;
import iskallia.vault.block.entity.VaultPortalTileEntity;
import iskallia.vault.item.crystal.CrystalData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StructureCommands extends BaseCommand {
    public String getName() {
        return "structures";
    }

    public void build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.literal("activateStructureWithIndex").then(Commands.argument("index", IntegerArgumentType.integer()).executes(this::activateStructureWithIndex)));
        builder.then(Commands.literal("removeStructureWithIndex").then(Commands.argument("index", IntegerArgumentType.integer()).executes(this::removeStructureWithIndex)));
        builder.then(Commands.literal("getStructureWithIndex").then(Commands.argument("index", IntegerArgumentType.integer()).executes(this::getStructureWithIndex)));
        builder.then(Commands.literal("getStructureList").then(Commands.argument("dimension", DimensionArgument.dimension()).executes(this::getStructureList)));
        builder.then(Commands.literal("getClosestStructure").executes(this::getClosestStructure));
        builder.then(Commands.literal("getRandomStructure").executes(this::getRandomStructure));
        builder.then(Commands.literal("getActiveOverVault").executes(this::getActiveOverVault));
        builder.then(Commands.literal("getNextOverVaultSpawn").executes(this::getNextOverVaultSpawn));
        builder.then(Commands.literal("activateAllPortals").executes(this::activateAllPortals));
        builder.then(Commands.literal("activateRandomPortal").executes(this::activateRandomPortal));
        builder.then(Commands.literal("deactivateActivePortal").executes(this::deactivateActivePortal));
        builder.then(Commands.literal("instantlyRunModifierRemoval").executes(this::instantlyRunModifierRemoval));
    }

    private int getStructureList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = DimensionArgument.getDimension(context, "dimension");

        MutableComponent cmp = new TextComponent("===")
                .append(new TextComponent(" Portal Data List ").withStyle(ChatFormatting.AQUA))
                .append(new TextComponent("==="));

        List<PortalData> originalList = PortalSavedData.get(level).getPortalData(); // Get the merged list

        int count = 0;
        for(PortalData filteredData : PortalUtil.filteredPortalList(level)) {
            cmp.append("\n");
            int index = originalList.indexOf(filteredData);

            BlockPos offsetPosition =  filteredData.getPortalFrameCenterPos().offset(3.0, 0.0, 3.0);
            String tpCommand = "/execute as @s in " + filteredData.getDimension().location() + " run tp " + offsetPosition.getX() + " " + offsetPosition.getY() + " " + offsetPosition.getZ();
            MutableComponent tpComponent = new TextComponent(" [Teleport!]").withStyle(ChatFormatting.AQUA);
            tpComponent.withStyle((style) -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("tpCommand"))).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand)));
            cmp.append(new TextComponent("Index " + index + ". ").withStyle(ChatFormatting.YELLOW));
            cmp.append(new TextComponent("X: " + filteredData.getPortalFrameCenterPos().getX() + " Y: " + filteredData.getPortalFrameCenterPos().getY() + " Z: " + filteredData.getPortalFrameCenterPos().getZ()));
            cmp.append(tpComponent);
            count++;
        }

        if(count >= 99) {
            cmp.append(new TextComponent("\n Only displayed the first 100 Entries").withStyle(ChatFormatting.RED));
            return 1;
        }

        context.getSource().sendSuccess(cmp, false);

        return 0;
    }


    private int getNextOverVaultSpawn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if(PortalSavedData.get(context.getSource().getServer()).getFirstActivePortalData() != null) {
            source.sendFailure(new TextComponent("An OverVaults portal is already active! Cannot spawn additional."));
            return 1;
        }

        int[] timeRemaining = MiscUtil.convertTime((ServerTickEvent.actlTicksForPortalSpawn - ServerTickEvent.counter)/20);
        int[] timeRequired = MiscUtil.convertTime(ServerTickEvent.actlTicksForPortalSpawn/20);

        String timeFormatted = MiscUtil.formatTime(timeRemaining);
        String defTimeFormatted = MiscUtil.formatTime(timeRequired);

        MutableComponent cmp = new TextComponent("")
                .append(new TextComponent("Time until next OverVault portal spawn: ").withStyle(ChatFormatting.GREEN))
                .append(new TextComponent(defTimeFormatted + "/")).withStyle(ChatFormatting.GRAY)
                .append(new TextComponent(timeFormatted).withStyle(ChatFormatting.AQUA))
                .append(new TextComponent(" (HH:MM:SS)").withStyle(ChatFormatting.GRAY));



        source.sendSuccess(cmp, true);
        return 0;
    }

    private int getActiveOverVault(CommandContext<CommandSourceStack> context) {
        PortalData data = PortalSavedData.get(context.getSource().getServer()).getFirstActivePortalData();

        if(data == null) {
            context.getSource().sendFailure(new TextComponent("No active OverVault found!"));
            return 1;
        }

        context.getSource().sendSuccess(TextUtil.getPortalTpComponent(PortalSavedData.get(context.getSource().getServer()).getPortalData().indexOf(data), data), true);
        return 0;
    }

    private int getStructureWithIndex(CommandContext<CommandSourceStack> context) {
        int index = IntegerArgumentType.getInteger(context, "index");
        List<PortalData> portalDataList = PortalSavedData.getServer().getPortalData();

        if (index >= portalDataList.size()) {
            context.getSource().sendFailure(new TextComponent("Index: " + index + " is out of bounds! Max allowed value is: " + (portalDataList.size() - 1)));
            return 1;
        }

        PortalData data = PortalSavedData.getServer().getPortalData().get(index);
        context.getSource().sendSuccess(TextUtil.getPortalTpComponent(index, data), true);
        return 0;
    }

    private int removeStructureWithIndex(CommandContext<CommandSourceStack> context) {
        int index = IntegerArgumentType.getInteger(context, "index");
        PortalSavedData portalSavedData = PortalSavedData.getServer();
        List<PortalData> portalDataList = portalSavedData.getPortalData();

        if (index >= portalDataList.size()) {
            context.getSource().sendFailure(new TextComponent("Index: " + index + " is out of bounds! Max allowed value is: " + (portalDataList.size() - 1)));
            return 1;
        }


        PortalData portalToRemove = portalDataList.get(index);
        portalDataList.remove(portalToRemove);

        MutableComponent cmp = new TextComponent("Removed Portal with index: " + index + "\n")
                .append(new TextComponent("(Note: This portal will be re-added to the List when the chunks are re-loaded)").withStyle(ChatFormatting.GRAY));

        context.getSource().sendSuccess(cmp, true);
        return 0;
    }

    public int getClosestStructure(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<PortalData> originalList = PortalSavedData.getServer().getPortalData();
        Pair<PortalData, Double> closestPortalData = PortalUtil.getClosestPortalData(player);

        if (closestPortalData == null) {
            context.getSource().sendFailure(new TextComponent("No valid portal data found in the dimension of the Player."));
            return 1;
        }

        PortalData portalData = closestPortalData.getFirst();
        int index = originalList.indexOf(portalData);
        double distance = closestPortalData.getSecond();

        MutableComponent cmp = TextUtil.getPortalTpComponent(index, PortalSavedData.get(player.getLevel()).getPortalData().get(index));
        cmp.append(new TextComponent("    Distance to closest Portal: " + (int) distance + " Block" + (distance <= 1 ? "" : "s")).withStyle(ChatFormatting.YELLOW));

        context.getSource().sendSuccess(cmp, true);
        return 0;
    }

    public int deactivateActivePortal(CommandContext<CommandSourceStack> context) {
        PortalData data = PortalSavedData.get(context.getSource().getServer()).getFirstActivePortalData();
        MinecraftServer srv = context.getSource().getServer();

        if (data == null) {
            context.getSource().sendFailure(new TextComponent("Found no active Portal!"));
            return 1;
        }

        ServerLevel level = srv.getLevel(data.getDimension());

        if (level == null) {
            context.getSource().sendFailure(new TextComponent("Invalid dimension...?"));
            return 1;
        }


        BlockEntityChunkSavedData entityChunkData = BlockEntityChunkSavedData.get(level);
        PortalSavedData savedData = PortalSavedData.get(level);

        for(BlockPos pos : entityChunkData.getPortalTilePositions()) {
            if(level.isLoaded(pos)) {
                level.removeBlock(pos, false);
            } else {
                OverVaults.LOGGER.error("Position {} not loaded when deactivating portal!", pos);
            }
        }

        entityChunkData.removePortalTileEntityData();
        entityChunkData.setMarkedForRemoval(false);
        data.setActiveState(false);
        data.setModifiersRemoved(-1);
        savedData.setDirty();
        for (ChunkPos chunkPos : entityChunkData.getForceloadedChunks()) {
            level.setChunkForced(chunkPos.x, chunkPos.z, false);
            level.getChunkSource().removeRegionTicket(OverVaultConstants.OVERVAULT_TICKET, chunkPos, 2, chunkPos);
        }
        entityChunkData.removeForceLoadedChunkData();

        for (ServerPlayer sP : srv.getPlayerList().getPlayers()) {
            if (!sP.getLevel().dimension().location().getNamespace().equals("the_vault")) {
                MiscUtil.clearCompassInfoForPlayer(sP);
            }
        }

        context.getSource().sendSuccess(new TextComponent("Removed active OverVault on position " + data.getPortalFrameCenterPos()).withStyle(ChatFormatting.YELLOW), true);
        return 0;
    }

    private int getRandomStructure(CommandContext<CommandSourceStack> context) {
        List<PortalData> portalDataList = PortalSavedData.get(context.getSource().getServer()).getPortalData();

        int index = new Random().nextInt(portalDataList.size());
        PortalData data = portalDataList.get(index);
        context.getSource().sendSuccess(TextUtil.getPortalTpComponent(index, data), true);
        return 0;
    }

    private int activateAllPortals(CommandContext<CommandSourceStack> context){
        MinecraftServer server = context.getSource().getServer();
        BlockEntityChunkSavedData entityChunkData = BlockEntityChunkSavedData.get(server);
        PortalSavedData portalSavedData = PortalSavedData.get(server);
        List<PortalData> portalDataList = new ArrayList<>(portalSavedData.getPortalData());
        Random rand = new Random();

        for(PortalData data : portalDataList) {
            PortalEntry portalEntry = VaultConfigRegistry.OVERVAULTS_PORTAL_CONFIG.PORTAL_LIST.getRandom(rand);
            if(portalEntry == null) {
                continue;
            }

            Pair<PortalEntry, CrystalData> pairEntry = OverVaultsPortalConfig.getRandomCrystalData(data.getDimension());
            List<VaultPortalTileEntity> portalTileEntities = PortalUtil.portalTileActivation(server.getLevel(data.getDimension()), data, pairEntry.getSecond());

            for (VaultPortalTileEntity portalTileEntity : portalTileEntities) {
                entityChunkData.addPortalTileEntity(portalTileEntity.getBlockPos());
            }

            data.setActiveState(true);
            entityChunkData.setDirty();
            portalSavedData.setDirty();
            MiscUtil.notifyPlayers(server, data, pairEntry.getFirst().getTranslationComponent());
        }

        return 0;
    }

    private int activateRandomPortal(CommandContext<CommandSourceStack> context) {
        if(PortalSavedData.get(ServerLifecycleHooks.getCurrentServer()).getFirstActivePortalData() != null) {
            context.getSource().sendFailure(new TextComponent("An OverVaults portal is already active! Cannot set activation timer."));
            return 1;
        }

        context.getSource().sendSuccess(new TextComponent("Instantly activated a Portal!").withStyle(ChatFormatting.YELLOW), true);
        ServerTickEvent.counter = ServerTickEvent.actlTicksForPortalSpawn;
        return 0;
    }

    private int activateStructureWithIndex(CommandContext<CommandSourceStack> context) {
        int index = IntegerArgumentType.getInteger(context, "index");
        PortalSavedData portalSavedData = PortalSavedData.getServer();
        if(portalSavedData.getFirstActivePortalData() != null) {
            context.getSource().sendFailure(new TextComponent("An OverVaults portal is already active!"));
            return 1;
        }

        List<PortalData> portalDataList = portalSavedData.getPortalData();

        if (index >= portalDataList.size()) {
            context.getSource().sendFailure(new TextComponent("Index: " + index + " is out of bounds! Max allowed value is: " + (portalDataList.size() - 1)));
            return 1;
        }

        PortalData portalToOpen = portalDataList.get(index);
        boolean active = PortalUtil.activatePortal(ServerLifecycleHooks.getCurrentServer(), portalToOpen);
        if(active) {
            context.getSource().sendSuccess(new TextComponent("Activated Portal with index: " + index).withStyle(ChatFormatting.YELLOW), true);
            return 0;
        }

        context.getSource().sendFailure(new TextComponent("Could not activate the given portal. Check logs"));
        return 1;
    }

    private int instantlyRunModifierRemoval(CommandContext<CommandSourceStack> context) {
        PortalSavedData portalSavedData = PortalSavedData.getServer();
        if(portalSavedData.getFirstActivePortalData() != null) {
            context.getSource().sendSuccess(new TextComponent("Instantly modified an active Portal!").withStyle(ChatFormatting.YELLOW), true);
            ServerTickEvent.activePortalTickCounter = ServerTickEvent.actlRemoveModifierTimer;
            return 0;
        } else {
            context.getSource().sendFailure(new TextComponent("Found no active Portal Data!"));
            return 1;
        }
    }
}
