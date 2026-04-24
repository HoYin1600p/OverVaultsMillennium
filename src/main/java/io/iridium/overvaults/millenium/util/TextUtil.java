package io.iridium.overvaults.millenium.util;

import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.millenium.world.PortalData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class TextUtil {
    public static MutableComponent getPortalComponent(int index, PortalData data) {
        return new TextComponent("=== ")
                .append(new TextComponent("Portal data of index " + index).withStyle(ChatFormatting.AQUA))
                .append(new TextComponent(" ===\n").withStyle(ChatFormatting.WHITE))
                .append(new TextComponent("    Portal Frame Center Position: ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(new TextComponent(data.getPortalFrameCenterPos() + "\n").withStyle(ChatFormatting.YELLOW))
                .append(new TextComponent("    Portal size: ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(new TextComponent(data.getSize() + "\n").withStyle(ChatFormatting.YELLOW))
                .append(new TextComponent("    Portal Rotation: ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(new TextComponent(data.getRotation() + "\n").withStyle(ChatFormatting.YELLOW))
                .append(new TextComponent("    Portal Activation State: ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(new TextComponent(data.getActiveState() + "\n").withStyle(ChatFormatting.YELLOW))
                .append(new TextComponent("    Portal Dimension: ").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(new TextComponent(data.getDimension().location().getPath() + "\n").withStyle(ChatFormatting.YELLOW));
    }

    public static MutableComponent getPortalTpComponent(int index, PortalData data) {
        BlockPos offsetPosition =  data.getPortalFrameCenterPos().offset(3.0, 0.0, 3.0);
        String tpCommand = "    /execute as @s in " + data.getDimension().location() + " run tp " + offsetPosition.getX() + " " + offsetPosition.getY() + " " + offsetPosition.getZ();
        MutableComponent tpComponent = new TextComponent(tpCommand).withStyle(ChatFormatting.AQUA);
        tpComponent.withStyle((style) -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to teleport!"))).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand)));

        return TextUtil.getPortalComponent(index, data).append(tpComponent);
    }

    public static MutableComponent loginComponent() {
        return new TranslatableComponent("overvaults.portal.login");
    }

    public static String dimensionName(ResourceKey<Level> dimension) {
        if(dimension.equals(Level.OVERWORLD)) {
            return "the Overworld";
        } else if (dimension.equals(Level.NETHER)) {
            return "the Nether";
        } else if (dimension.equals(Level.END)) {
            return "the End";
        } else {
            OverVaults.LOGGER.error("Chosen dimension for a new active portal was not the overworld, nether, or end.");
            return "Report this";
        }
    }

    public static MutableComponent dimensionComponent(ResourceKey<Level> dimension) {
        if(dimension.equals(Level.OVERWORLD)) {
            return new TextComponent(dimensionName(dimension)).withStyle(ChatFormatting.YELLOW);
        } else if (dimension.equals(Level.NETHER)) {
            return new TextComponent(dimensionName(dimension)).withStyle(ChatFormatting.RED);
        } else if (dimension.equals(Level.END)) {
            return new TextComponent(dimensionName(dimension)).withStyle(ChatFormatting.DARK_AQUA);
        } else {
            OverVaults.LOGGER.error("Chosen dimension for a new active portal was not the overworld, nether, or end.");
            return new TextComponent("Report this").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.DARK_RED);

        }
    }
}
