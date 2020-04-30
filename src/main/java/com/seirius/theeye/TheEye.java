package com.seirius.theeye;

import com.seirius.theeye.common.TestTheMap;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.block.Block;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

@Mod("theeye")
public class TheEye {
    private static final Logger LOGGER = LogManager.getLogger();

    public TheEye() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().gameSettings);
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        InterModComms.sendTo("theeye", "helloworld", () -> {
            LOGGER.info("Hello from the SeiRiuS");
            return "The eye is watching them";
        });
    }

    private void processIMC(final InterModProcessEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        final ServerWorld world = event.getServer().getWorld(DimensionType.OVERWORLD);
        System.out.println("--------------- ServerSTARTUP");
        TestTheMap.register(event.getCommandDispatcher());
        try {
            final String apiMap = "/api/map/";
            HttpServer server = HttpServer.create(new InetSocketAddress(9000), 0);
            server.createContext(apiMap, (httpExchange -> {
                try {
                    String path = httpExchange.getRequestURI().getPath();
                    String params = path.substring(apiMap.length()).replace(".png", "");
                    String[] zoomXZ = params.split("/");
                    int x = Integer.parseInt(zoomXZ[1]);
                    int z = Integer.parseInt(zoomXZ[2]);
                    byte[] image = TestTheMap.getChunkImageAsBytes(world, x * TestTheMap.CHUNK_SIZE, z * TestTheMap.CHUNK_SIZE);
                    httpExchange.getResponseHeaders().set("Content-Type", "image/png");
                    httpExchange.sendResponseHeaders(200, image.length);
                    OutputStream output = httpExchange.getResponseBody();
                    output.write(image);
                    output.close();
                    httpExchange.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
            server.setExecutor(null);
            server.start();
            System.out.println("Server started");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
        }
    }

}
