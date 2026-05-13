package io.iridium.overvaults.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import io.iridium.overvaults.OverVaults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OvervaultStructurePreviewRenderer {
    private static final Map<ResourceLocation, PreviewStructure> CACHE = new HashMap<>();

    public static void render(PoseStack poseStack, ResourceLocation structureId, int centerX, int centerY, int maxSize, float yaw, float pitch) {
        PreviewStructure structure = getStructure(structureId);
        if (structure == null || structure.blocks().isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        poseStack.pushPose();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();

        float modelSize = Math.max(1.0F, structure.maxDimension());
        float scale = maxSize / (modelSize * 1.65F);
        poseStack.translate(centerX, centerY + maxSize * 0.10F, 220.0F);
        poseStack.scale(scale, -scale, scale);
        poseStack.mulPose(Vector3f.XP.rotationDegrees(25.0F + pitch));
        poseStack.mulPose(Vector3f.YP.rotationDegrees(yaw));
        poseStack.translate(-structure.centerX(), -structure.centerY(), -structure.centerZ());

        for (PreviewBlock block : structure.blocks()) {
            poseStack.pushPose();
            BlockPos pos = block.pos();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            blockRenderer.renderSingleBlock(block.state(), poseStack, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        bufferSource.endBatch();
        Lighting.setupFor3DItems();
        RenderSystem.disableDepthTest();
        poseStack.popPose();
    }

    private static PreviewStructure getStructure(ResourceLocation structureId) {
        if (CACHE.containsKey(structureId)) {
            return CACHE.get(structureId);
        }

        PreviewStructure structure = loadStructure(structureId);
        CACHE.put(structureId, structure);
        return structure;
    }

    private static PreviewStructure loadStructure(ResourceLocation structureId) {
        String path = "data/" + structureId.getNamespace() + "/structures/" + structureId.getPath() + ".nbt";
        try (InputStream stream = OvervaultStructurePreviewRenderer.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                OverVaults.LOGGER.error("Could not load OverVault GUI structure '{}': resource '{}' was not found.", structureId, path);
                return null;
            }

            return readStructure(NbtIo.readCompressed(stream));
        } catch (IOException exception) {
            OverVaults.LOGGER.error("Could not load OverVault GUI structure '{}'.", structureId, exception);
            return null;
        }
    }

    private static PreviewStructure readStructure(CompoundTag root) {
        ListTag paletteTag = root.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(NbtUtils.readBlockState(paletteTag.getCompound(i)));
        }

        List<PreviewBlock> blocks = new ArrayList<>();
        ListTag blocksTag = root.getList("blocks", Tag.TAG_COMPOUND);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompound(i);
            int stateIndex = blockTag.getInt("state");
            if (stateIndex < 0 || stateIndex >= palette.size()) {
                continue;
            }

            ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
            if (posTag.size() < 3) {
                continue;
            }

            BlockPos pos = new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2));
            blocks.add(new PreviewBlock(pos, palette.get(stateIndex)));
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        if (blocks.isEmpty()) {
            return new PreviewStructure(blocks, 0.0F, 0.0F, 0.0F, 1.0F);
        }

        float centerX = (minX + maxX + 1.0F) / 2.0F;
        float centerY = (minY + maxY + 1.0F) / 2.0F;
        float centerZ = (minZ + maxZ + 1.0F) / 2.0F;
        float maxDimension = Math.max(maxX - minX + 1, Math.max(maxY - minY + 1, maxZ - minZ + 1));
        return new PreviewStructure(blocks, centerX, centerY, centerZ, maxDimension);
    }

    private record PreviewStructure(List<PreviewBlock> blocks, float centerX, float centerY, float centerZ, float maxDimension) {
    }

    private record PreviewBlock(BlockPos pos, BlockState state) {
    }
}
