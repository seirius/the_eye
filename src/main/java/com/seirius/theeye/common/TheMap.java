package com.seirius.theeye.common;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TheMap {

    public static int CHUNK_SIZE = 16;

    public static int MAX_ZOOM = 12;

    public static int transformZoom(int incommingZoom) {
        return MAX_ZOOM - incommingZoom + 1;
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
            Commands.literal("testmap").requires((permission) -> permission.hasPermissionLevel(4))
                .then(Commands.literal("tiles").executes((command) -> 1))
        );
    }

    public static BufferedImage getChunkImage(ServerWorld world, int x, int z, int zoom) {
        int zoomedSize = CHUNK_SIZE * zoom;
        int[] pixels = new int[zoomedSize * zoomedSize];

        int worldStartZ = z * zoomedSize;
        int worldStartX = x * zoomedSize;

        int minZ = z * zoomedSize;
        int minX = x * zoomedSize;
        int currentChunkX = resolveCoorForChunk(minX) / CHUNK_SIZE;
        int currentChunkZ = resolveCoorForChunk(minZ) / CHUNK_SIZE;

        ChunkStorage chunkStorage = new ChunkStorage(world);

        IChunk chunk = chunkStorage.getChunk(minX, minZ);
        for (int imageZ = 0; imageZ < zoomedSize; imageZ++) {
            int worldZ = imageZ + worldStartZ;
            for (int imageX = 0; imageX < zoomedSize; imageX++) {
                int worldX = imageX + worldStartX;

                int newCurrentX = resolveCoorForChunk(worldX) / CHUNK_SIZE;
                int newCurrentZ = resolveCoorForChunk(worldZ) / CHUNK_SIZE;
                if (newCurrentX != currentChunkX || newCurrentZ != currentChunkZ) {
                    currentChunkX = newCurrentX;
                    currentChunkZ = newCurrentZ;
                    chunk = chunkStorage.getChunk(worldX, worldZ);
                }
                int topY = chunk.getTopBlockY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ);
                BlockState blockState = chunk.getBlockState(new BlockPos(worldX, topY, worldZ));
                int color = blockState.getMaterial().getColor().colorValue;
                float colorFactor = getColorFactor(topY);
                pixels[imageZ * zoomedSize + imageX] = manipulateColor(color, colorFactor);
            }
        }

        BufferedImage pixelImage = new BufferedImage(zoomedSize, zoomedSize, BufferedImage.TYPE_INT_RGB);
        pixelImage.setRGB(0, 0, zoomedSize, zoomedSize, pixels, 0, zoomedSize);

        return TheMap.resizeImage(pixelImage, 160, 160);
    }

    private static int resolveCoorForChunk(int coor) {
        return coor < 0 ? ++coor : coor;
    }

    public static byte[] getChunkImageAsBytes(ServerWorld world, int x, int z, int zoom) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(getChunkImage(world, x, z, zoom), "png", baos);
        baos.flush();
        byte[] byteArray = baos.toByteArray();
        baos.flush();
        return byteArray;
    }

    private static BufferedImage resizeImage(BufferedImage buf, int width, int height) {
        final BufferedImage bufImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2 = bufImage.createGraphics();
        g2.drawImage(buf, 0, 0, width, height, null);
        g2.dispose();
        return bufImage;
    }

    public static float getColorFactor(int y) {
        float oceanLevel = 62f;
        float rawFactor = oceanLevel / y;
        if (rawFactor < 1) {
            return 2 - rawFactor;
        } else {
            return (rawFactor - 2) * -1;
        }
    }

    public static int manipulateColor(int color, float factor) {
        Color col = new Color(color);
        int r, g, b;
        if (factor <= 1) {
            r = Math.round(col.getRed() * factor);
            g = Math.round(col.getGreen() * factor);
            b = Math.round(col.getBlue() * factor);
        } else {
            float difFactor = factor - 1;
            int red = col.getRed(), green = col.getGreen(), blue = col.getBlue();
            r = Math.round((255 - red) * difFactor + red);
            g = Math.round((255 - green) * difFactor + green);
            b = Math.round((255 - blue) * difFactor + blue);
        }

        int rgb = r;
        rgb = (rgb << 8) + g;
        rgb = (rgb << 8) + b;
        return rgb;
    }

}
