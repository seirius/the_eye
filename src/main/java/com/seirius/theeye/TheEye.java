package com.seirius.theeye;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.ByteStreams;
import com.seirius.theeye.common.PlayerListController;
import com.seirius.theeye.common.TheMap;
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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Mod("theeye")
public class TheEye {
    private static final Logger LOGGER = LogManager.getLogger();

    private final static LoadingCache<String, byte[]> IMAGE_MAP_CACHE = CacheBuilder.newBuilder()
            .maximumSize(3000)
            .expireAfterAccess(12, TimeUnit.HOURS)
            .build(
                    new CacheLoader<String, byte[]>() {
                        @Override
                        public byte[] load(String key) throws Exception {
                            int[] data = Arrays.stream(key.split(":")).mapToInt(Integer::parseInt).toArray();
                            return TheMap.getChunkImageAsBytes(WORLD, data[0], data[1], data[2]);
                        }
                    }
            );

    public static ServerWorld WORLD;

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
        WORLD = event.getServer().getWorld(DimensionType.OVERWORLD);
        System.out.println("--------------- ServerSTARTUP");
        TheMap.register(event.getCommandDispatcher());
        try {
            final String apiMap = "/api/map/";
            final String map = "/";
            HttpServer server = HttpServer.create(new InetSocketAddress(9000), 0);
            server.createContext(PlayerListController.PATH,  new PlayerListController(WORLD));
            server.createContext(map, (httpExchange -> {
                try {
                    byte[] indexHtml = ByteStreams.toByteArray(TheEye.class.getResourceAsStream("/index.html"));
                    ClassLoader.getSystemClassLoader().getResource("index.html");
                    httpExchange.getResponseHeaders().set("Content-Type", "text/html");
                    httpExchange.sendResponseHeaders(200, indexHtml.length);
                    OutputStream output = httpExchange.getResponseBody();
                    output.write(indexHtml);
                    output.close();
                    httpExchange.close();
                } catch (Exception e) {
                    if (e instanceof IOException) {
                        return;
                    }
                    e.printStackTrace();
                }
            }));
            server.createContext(apiMap, (httpExchange -> {
                try {
                    String path = httpExchange.getRequestURI().getPath();
                    String params = path.substring(apiMap.length()).replace(".png", "");
                    String[] zoomXZ = params.split("/");
                    int zoom = Integer.parseInt(zoomXZ[0]);
                    int x = Integer.parseInt(zoomXZ[1]);
                    int z = Integer.parseInt(zoomXZ[2]);
                    byte[] image = IMAGE_MAP_CACHE.get(getKey(x, z, TheMap.transformZoom(zoom)));
                    httpExchange.getResponseHeaders().set("Content-Type", "image/png");
                    httpExchange.sendResponseHeaders(200, image.length);
                    OutputStream output = httpExchange.getResponseBody();
                    output.write(image);
                    output.close();
                    httpExchange.close();
                } catch (Exception e) {
                    if (e instanceof IOException) {
                        return;
                    }
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

    public static String getKey(int x, int z, int zoom) {
        return String.format("%d:%d:%d", x, z, zoom);
    }

}
