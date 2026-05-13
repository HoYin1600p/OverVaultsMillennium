package io.iridium.overvaults.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.network.ClientboundOvervaultGuiDataPacket;
import io.iridium.overvaults.network.OverVaultsNetwork;
import io.iridium.overvaults.network.ServerboundOvervaultGuiClosePacket;
import io.iridium.overvaults.network.ServerboundOvervaultGuiRequestPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.List;

public class OvervaultPortalScreen extends Screen {
    private static final int TITLE_SUPPORT = 0xB7E3FF;
    private static final int LABEL_COLOR = 0xA8C0CF;
    private static final int VALUE_COLOR = 0xEAF4FF;
    private static final int SECONDARY_COLOR = 0x7F8C96;
    private static final int PLAYER_LEVEL_COLOR = 0xE6D28A;
    private static final ResourceLocation OVERWORLD_STRUCTURE = new ResourceLocation(OverVaults.MOD_ID, "gui_portal_overworld");
    private static final ResourceLocation NETHER_STRUCTURE = new ResourceLocation(OverVaults.MOD_ID, "gui_portal_nether");
    private static final ResourceLocation END_STRUCTURE = new ResourceLocation(OverVaults.MOD_ID, "gui_portal_end");
    private static final ResourceLocation INACTIVE_STRUCTURE = new ResourceLocation(OverVaults.MOD_ID, "gui_portal_inactive");

    private ClientboundOvervaultGuiDataPacket data = new ClientboundOvervaultGuiDataPacket(true, false, "inactive", "Unknown", 0x00AAAA, 30000, -1, List.of(), 0, "");
    private float previewYaw = 35.0F;
    private float previewPitch = 0.0F;
    private boolean draggingPreview;

    public OvervaultPortalScreen() {
        super(new TranslatableComponent("screen.overvaults.overvault_gui"));
    }

    @Override
    protected void init() {
        int buttonWidth = 90;
        int buttonX = this.width / 2 - buttonWidth / 2;
        int buttonY = this.height - 28;
        this.addRenderableWidget(new Button(buttonX, buttonY, buttonWidth, 20, new TranslatableComponent("button.overvaults.refresh"), button -> requestRefresh()));
        requestRefresh();
    }

    public void applyData(ClientboundOvervaultGuiDataPacket data) {
        this.data = data;
    }

    @Override
    public void removed() {
        OverVaultsNetwork.CHANNEL.sendToServer(new ServerboundOvervaultGuiClosePacket());
        super.removed();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        renderPortalStructure(poseStack);
        renderInfo(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void requestRefresh() {
        OverVaultsNetwork.CHANNEL.sendToServer(new ServerboundOvervaultGuiRequestPacket());
    }

    private void renderPortalStructure(PoseStack poseStack) {
        int previewSize = getPreviewSize();
        OvervaultStructurePreviewRenderer.render(poseStack, getPortalStructure(), this.width / 4, this.height / 2, previewSize, this.previewYaw, this.previewPitch);
    }

    private ResourceLocation getPortalStructure() {
        if (!this.data.active) {
            return INACTIVE_STRUCTURE;
        }

        if (Level.NETHER.location().toString().equals(this.data.dimensionId)) {
            return NETHER_STRUCTURE;
        }

        if (Level.END.location().toString().equals(this.data.dimensionId)) {
            return END_STRUCTURE;
        }

        return OVERWORLD_STRUCTURE;
    }

    private int getPreviewSize() {
        int leftWidth = this.width / 2;
        int maxWidth = Math.max(32, leftWidth - 40);
        int maxHeight = Math.max(32, this.height - 70);
        return Math.min(maxWidth, maxHeight);
    }

    private boolean isInPreviewArea(double mouseX, double mouseY) {
        int previewSize = getPreviewSize();
        int left = this.width / 4 - previewSize / 2;
        int top = this.height / 2 - previewSize / 2;
        return mouseX >= left && mouseX <= left + previewSize && mouseY >= top && mouseY <= top + previewSize;
    }

    private void renderInfo(PoseStack poseStack) {
        int rightCenterX = this.width * 3 / 4;
        int y = 28;

        Component title = this.data.active ? getActiveTitle() : new TextComponent("No Portal Activity Detected").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(TITLE_SUPPORT)));
        drawCenteredString(poseStack, this.font, title, rightCenterX, y, TITLE_SUPPORT);

        y += 38;
        drawCenteredString(poseStack, this.font, new TextComponent("Vault Timer").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(LABEL_COLOR))), rightCenterX, y, LABEL_COLOR);
        y += 14;
        drawCenteredString(poseStack, this.font, new TextComponent(formatTicks(this.data.vaultTimerTicks)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(VALUE_COLOR))), rightCenterX, y, VALUE_COLOR);

        y += 30;
        drawCenteredString(poseStack, this.font, new TextComponent("Portal Level").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(LABEL_COLOR))), rightCenterX, y, LABEL_COLOR);
        y += 14;
        String levelText = this.data.vaultLevel >= 0 ? String.valueOf(this.data.vaultLevel) : "(undetermined)";
        int levelColor = this.data.vaultLevel >= 0 ? VALUE_COLOR : SECONDARY_COLOR;
        drawCenteredString(poseStack, this.font, new TextComponent(levelText).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(levelColor))), rightCenterX, y, levelColor);

        y += 32;
        renderPlayers(poseStack, rightCenterX, y);

        y = Math.max(y + 55 + this.data.players.size() * 12, this.height - 58);
        drawCenteredString(poseStack, this.font, new TextComponent("Portal Active").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(LABEL_COLOR))), rightCenterX, y, LABEL_COLOR);
        drawCenteredString(poseStack, this.font, new TextComponent(formatTicks(this.data.portalActiveTicks)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(VALUE_COLOR))), rightCenterX, y + 14, VALUE_COLOR);
    }

    private Component getActiveTitle() {
        return new TextComponent("")
                .append(new TextComponent(this.data.rank + "-Rank").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.data.rankColor))))
                .append(new TextComponent(" Portal is Active").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(TITLE_SUPPORT))));
    }

    private void renderPlayers(PoseStack poseStack, int rightCenterX, int y) {
        int levelX = rightCenterX - 72;
        int nameX = rightCenterX - 10;
        this.font.drawShadow(poseStack, new TextComponent("Level").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(LABEL_COLOR))), levelX, y, LABEL_COLOR);
        this.font.drawShadow(poseStack, new TextComponent("Name").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(LABEL_COLOR))), nameX, y, LABEL_COLOR);

        y += 14;
        if (this.data.players.isEmpty()) {
            drawCenteredString(poseStack, this.font, new TextComponent("No players inside").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(SECONDARY_COLOR))), rightCenterX, y, SECONDARY_COLOR);
            return;
        }

        for (ClientboundOvervaultGuiDataPacket.PlayerEntry player : this.data.players) {
            this.font.drawShadow(poseStack, String.valueOf(player.vaultLevel()), levelX, y, PLAYER_LEVEL_COLOR);
            this.font.drawShadow(poseStack, player.name(), nameX, y, VALUE_COLOR);
            y += 12;
        }
    }

    private String formatTicks(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInPreviewArea(mouseX, mouseY)) {
            this.draggingPreview = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingPreview) {
            this.draggingPreview = false;
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingPreview) {
            this.previewYaw += (float) dragX * 0.75F;
            this.previewPitch = Mth.clamp(this.previewPitch + (float) dragY * 0.45F, -35.0F, 35.0F);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
