package com.seirius.theeye.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayerListController implements HttpHandler {

    public static final String PATH = "/api/players";

    private final ServerWorld world;

    public PlayerListController(ServerWorld world) {
        this.world = world;
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            List<ServerPlayerEntity> players = world.getPlayers();
            List<HashMap<String, Object>> playerList = new ArrayList<>();
            for (ServerPlayerEntity player : players) {
                HashMap<String, Object> playerData = new HashMap<>();
                playerData.put("name", player.getDisplayName().getString());
                playerData.put("position", player.getPositionVec());
                playerData.put("nameUuid", player.getDisplayNameAndUUID().getString());
                playerList.add(playerData);
            }

            byte[] response = new ObjectMapper().writeValueAsBytes(playerList);

            httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,HEAD");
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            httpExchange.sendResponseHeaders(200, response.length);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpExchange.close();
        }
    }
}
