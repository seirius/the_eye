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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class TestTheMap {

    public static int CHUNK_SIZE = 16;

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

    public static BufferedImage getChunkImage(ServerWorld world, int x, int z) {
        Chunk chunk = getChunk(world, x, z);
        int[] pixels = new int[CHUNK_SIZE * CHUNK_SIZE];
        int maxZ = z + CHUNK_SIZE;
        int maxX = x + CHUNK_SIZE;
        int zCount = 0;
        for (int zIndex = z; zIndex < maxZ; zIndex++) {
            int xCount = 0;
            for (int xIndex = x; xIndex < maxX; xIndex++) {
                int topY = chunk.getTopBlockY(Heightmap.Type.WORLD_SURFACE, xIndex, zIndex);
                BlockState blockState = chunk.getBlockState(new BlockPos(xIndex, topY, zIndex));
                pixels[zCount * CHUNK_SIZE + xCount] = blockState.getMaterial().getColor().colorValue;
                xCount++;
            }
            zCount++;
        }
        BufferedImage pixelImage = new BufferedImage(CHUNK_SIZE, CHUNK_SIZE, BufferedImage.TYPE_INT_RGB);
        pixelImage.setRGB(0, 0, CHUNK_SIZE, CHUNK_SIZE, pixels, 0, CHUNK_SIZE);
        return pixelImage;
    }

    public static byte[] getChunkImageAsBytes(ServerWorld world, int x, int z) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(getChunkImage(world, x, z), "png", baos);
        baos.flush();
        byte[] byteArray = baos.toByteArray();
        baos.flush();
        return byteArray;
    }

    public static void main(String[] args) {
        System.out.println(2 << 4);
    }

}
