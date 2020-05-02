package com.seirius.theeye.common;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class TestTheMap {

    public static int CHUNK_SIZE = 16;

    public static int MAX_ZOOM = 12;

    public static int transformZoom(int incommingZoom) {
        return MAX_ZOOM - incommingZoom + 1;
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
            Commands.literal("testmap").requires((permission) -> permission.hasPermissionLevel(4))
                .then(Commands.literal("tiles").executes((command) -> {
                    try {
                        ServerWorld world = command.getSource().getWorld();
                        int chunkNumber = 16;
                        int size = chunkNumber * chunkNumber * CHUNK_SIZE;
                        int halfOffset = size / 2;
                        int[] pixels = new int[size * size];
                        for (int chunkX = -chunkNumber; chunkX < chunkNumber; chunkX++) {
                            for (int chunkZ = -chunkNumber; chunkZ < chunkNumber; chunkZ++) {
                                int minZ = chunkZ * CHUNK_SIZE;
                                int maxZ = minZ + CHUNK_SIZE;
                                int minX = chunkX * CHUNK_SIZE;
                                int maxX = minX + CHUNK_SIZE;
                                Chunk chunk = getChunk(world, minX, minZ);
                                for (int z = minZ; z < maxZ; z++) {
                                    for (int x = minX; x < maxX; x++) {
                                        int topY = chunk.getTopBlockY(Heightmap.Type.WORLD_SURFACE, x, z);
                                        BlockState blockState = chunk.getBlockState(new BlockPos(x, topY, z));
                                        pixels[(z + halfOffset) * size + (x + halfOffset)] = blockState.getMaterial().getColor().colorValue;
                                    }
                                }
                            }
                        }
                        BufferedImage pixelImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                        pixelImage.setRGB(0, 0, size, size, pixels, 0, size);
                        File file = new File("test.png");
                        ImageIO.write(pixelImage, "png", file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return 1;
                }))
        );
    }

    private static Chunk getChunk(ServerWorld world, int x, int z) {
        return world.getChunkAt(new BlockPos(x, 0, z));
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
        Chunk chunk = getChunk(world, minX, minZ);

        for (int imageZ = 0; imageZ < zoomedSize; imageZ++) {
            int worldZ = imageZ + worldStartZ;
            for (int imageX = 0; imageX < zoomedSize; imageX++) {
                int worldX = imageX + worldStartX;

                int newCurrentX = resolveCoorForChunk(worldX) / CHUNK_SIZE;
                int newCurrentZ = resolveCoorForChunk(worldZ) / CHUNK_SIZE;
                if (newCurrentX != currentChunkX || newCurrentZ != currentChunkZ) {
                    currentChunkX = newCurrentX;
                    currentChunkZ = newCurrentZ;
                    chunk = getChunk(world, worldX, worldZ);
                }
                int topY = chunk.getTopBlockY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ);
                BlockState blockState = chunk.getBlockState(new BlockPos(worldX, topY, worldZ));
                pixels[imageZ * zoomedSize + imageX] = blockState.getMaterial().getColor().colorValue;
            }
        }

        BufferedImage pixelImage = new BufferedImage(zoomedSize, zoomedSize, BufferedImage.TYPE_INT_RGB);
        pixelImage.setRGB(0, 0, zoomedSize, zoomedSize, pixels, 0, zoomedSize);

        return TestTheMap.resizeImage(pixelImage, 320, 320);
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

}
