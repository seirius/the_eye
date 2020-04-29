package com.seirius.theeye.common;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class TestTheMap {

    private static int CHUNK_SIZE = 16;

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

    public static void main(String[] args) {
        System.out.println(2 << 4);
    }

}
