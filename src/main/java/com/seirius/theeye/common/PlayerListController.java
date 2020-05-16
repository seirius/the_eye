package com.seirius.theeye.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
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
                String playerName = player.getDisplayName().getString();
                String uuid = player.getDisplayNameAndUUID().getString()
                        .replace(playerName + " ", "")
                        .replace("(", "")
                        .replace(")", "")
                        .replace("-", "");
                playerData.put("name", playerName);
                Vec3d position = player.getPositionVec();
                HashMap<String, Double> parsedPosition = new HashMap<>();
                parsedPosition.put("x", position.x);
                parsedPosition.put("y", position.y);
                parsedPosition.put("z", position.z);
                playerData.put("position", parsedPosition);
                playerData.put("uuid", uuid);
                playerData.put("avatar", String.format("https://mc-heads.net/avatar/%s/32.png", uuid));
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
