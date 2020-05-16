package com.seirius.theeye.common;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.server.ServerWorld;

import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;

public class PlayerIconController implements HttpHandler {

    public static final String PATH = "/api/players/icon/";

    private final ServerWorld world;

    public PlayerIconController(ServerWorld world) {
        this.world = world;
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            String path = httpExchange.getRequestURI().getPath();
            String playerUuid = path.substring(PATH.length());
            System.out.println(playerUuid);
            final PlayerEntity playerEntity = world.getPlayerByUuid(UUID.fromString(playerUuid));

//            skinManager.loadSkin(profiles.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
            System.out.println(playerEntity);
            String url = playerEntity.getGameProfile().getName();
            System.out.println(url);
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,HEAD");
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            httpExchange.sendResponseHeaders(200, url.getBytes().length);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(url.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
             httpExchange.close();
        }
    }

}
