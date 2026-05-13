package io.iridium.overvaults.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.iridium.overvaults.OverVaults;
import io.iridium.overvaults.network.ClientboundOvervaultGuiDataPacket;
import io.iridium.overvaults.network.OverVaultsNetwork;
import io.iridium.overvaults.network.ServerboundOvervaultGuiClosePacket;
import io.iridium.overvaults.network.ServerboundOvervaultGuiRequestPacket;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

public class OvervaultPortalScreen extends Screen {
    private static final int IMAGE_TEXTURE_SIZE = 512;
    private static final int LIGHT_BLUE = 0x8FDFFF;
    private static final ResourceLocation OVERWORLD_TEXTURE = new ResourceLocation(OverVaults.MOD_ID, "textures/gui/overvault_portal_overworld.png");
    private static final ResourceLocation NETHER_TEXTURE = new ResourceLocation(OverVaults.MOD_ID, "textures/gui/overvault_portal_nether.png");
    private static final ResourceLocation END_TEXTURE = new ResourceLocation(OverVaults.MOD_ID, "textures/gui/overvault_portal_end.png");
    private static final ResourceLocation INACTIVE_TEXTURE = new ResourceLocation(OverVaults.MOD_ID, "textures/gui/overvault_portal_inactive.png");

    private ClientboundOvervaultGuiDataPacket data = new ClientboundOvervaultGuiDataPacket(true, false, "inactive", "Unknown", 0x00AAAA, 30000, -1, List.of(), 0, "");

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
        renderPortalImage(poseStack);
        renderInfo(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void requestRefresh() {
        OverVaultsNetwork.CHANNEL.sendToServer(new ServerboundOvervaultGuiRequestPacket());
    }

    private void renderPortalImage(PoseStack poseStack) {
        int leftWidth = this.width / 2;
        int maxWidth = Math.max(32, leftWidth - 40);
        int maxHeight = Math.max(32, this.height - 70);
        int imageSize = Math.min(maxWidth, maxHeight);
        int x = leftWidth / 2 - imageSize / 2;
        int y = this.height / 2 - imageSize / 2;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, getPortalTexture());
        RenderSystem.enableBlend();
        GuiComponent.blit(poseStack, x, y, imageSize, imageSize, 0.0F, 0.0F, IMAGE_TEXTURE_SIZE, IMAGE_TEXTURE_SIZE, IMAGE_TEXTURE_SIZE, IMAGE_TEXTURE_SIZE);
        RenderSystem.disableBlend();
    }

    private ResourceLocation getPortalTexture() {
        if (!this.data.active) {
            return INACTIVE_TEXTURE;
        }

        if (Level.NETHER.location().toString().equals(this.data.dimensionId)) {
            return NETHER_TEXTURE;
        }

        if (Level.END.location().toString().equals(this.data.dimensionId)) {
            return END_TEXTURE;
        }

        return OVERWORLD_TEXTURE;
    }

    private void renderInfo(PoseStack poseStack) {
        int rightCenterX = this.width * 3 / 4;
        int y = 28;

        Component title = this.data.active ? getActiveTitle() : new TextComponent("No Portal Activity Detected").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(LIGHT_BLUE)));
        drawCenteredString(poseStack, this.font, title, rightCenterX, y, LIGHT_BLUE);

        y += 38;
        drawCenteredString(poseStack, this.font, new TextComponent("Vault Timer").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(LIGHT_BLUE))), rightCenterX, y, LIGHT_BLUE);
        y += 14;
        drawCenteredString(poseStack, this.font, new TextComponent(formatTicks(this.data.vaultTimerTicks)), rightCenterX, y, 0xFFFFFF);

        y += 30;
        drawCenteredString(poseStack, this.font, new TextComponent("Portal Level").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(LIGHT_BLUE))), rightCenterX, y, LIGHT_BLUE);
        y += 14;
        String levelText = this.data.vaultLevel >= 0 ? String.valueOf(this.data.vaultLevel) : "(undetermined)";
        drawCenteredString(poseStack, this.font, new TextComponent(levelText), rightCenterX, y, 0xFFFFFF);

        y += 32;
        renderPlayers(poseStack, rightCenterX, y);

        y = Math.max(y + 55 + this.data.players.size() * 12, this.height - 58);
        drawCenteredString(poseStack, this.font, new TextComponent("Portal Active").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(LIGHT_BLUE))), rightCenterX, y, LIGHT_BLUE);
        drawCenteredString(poseStack, this.font, new TextComponent(formatTicks(this.data.portalActiveTicks)), rightCenterX, y + 14, 0xFFFFFF);
    }

    private Component getActiveTitle() {
        return new TextComponent("")
                .append(new TextComponent(this.data.rank + "-Rank").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.data.rankColor))))
                .append(new TextComponent(" Portal is Active").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(LIGHT_BLUE))));
    }

    private void renderPlayers(PoseStack poseStack, int rightCenterX, int y) {
        int levelX = rightCenterX - 72;
        int nameX = rightCenterX - 10;
        this.font.drawShadow(poseStack, new TextComponent("Level").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(LIGHT_BLUE))), levelX, y, LIGHT_BLUE);
        this.font.drawShadow(poseStack, new TextComponent("Name").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(LIGHT_BLUE))), nameX, y, LIGHT_BLUE);

        y += 14;
        if (this.data.players.isEmpty()) {
            drawCenteredString(poseStack, this.font, new TextComponent("No players inside"), rightCenterX, y, 0xAAAAAA);
            return;
        }

        for (ClientboundOvervaultGuiDataPacket.PlayerEntry player : this.data.players) {
            this.font.drawShadow(poseStack, String.valueOf(player.vaultLevel()), levelX, y, 0xFFFFFF);
            this.font.drawShadow(poseStack, player.name(), nameX, y, 0xFFFFFF);
            y += 12;
        }
    }

    private String formatTicks(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
