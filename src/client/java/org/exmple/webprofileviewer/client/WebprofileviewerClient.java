package org.exmple.webprofileviewer.client;


import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class WebprofileviewerClient implements ClientModInitializer {
    private static final java.util.concurrent.Executor IO_EXEC = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("WebProfileChecker-IO");
        return thread;
    });

    public String extractBWStats(String Playername) throws Exception {
        String url = "https://hypixel.net/player/" + Playername;
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .get();
        //抓取各项数据部分：

        //4s final
        String finalKD4v4 = doc.select("#stats-content-bedwars td.statName").stream()
                .filter(td -> "4v4v4v4 Final K/D".equalsIgnoreCase(td.text().trim()))
                .findFirst()
                .map(Element::parent)                  // tr
                .map(tr -> tr.selectFirst("td.statValue"))
                .map(Element::text)
                .orElse("未找到");
        //2s final
        String finalKD2v2 = doc.select("#stats-content-bedwars td.statName").stream()
                .filter(td -> "Doubles Final K/D".equalsIgnoreCase(td.text().trim()))
                .findFirst()
                .map(Element::parent)                  // tr
                .map(tr -> tr.selectFirst("td.statValue"))
                .map(Element::text)
                .orElse("未找到");
        //total wins
        String totalWins = doc.select("#stats-content-bedwars td.statName").stream()
                .filter(td -> "Wins".equalsIgnoreCase(td.text().trim()))
                .findFirst()
                .map(Element::parent)                  // tr
                .map(tr -> tr.selectFirst("td.statValue"))
                .map(Element::text)
                .orElse("未找到");
        //final K/D
        String finalKD = doc.select("#stats-content-bedwars td.statName").stream()
                .filter(td -> "Final K/D".equalsIgnoreCase(td.text().trim()))
                .findFirst()
                .map(Element::parent)                  // tr
                .map(tr -> tr.selectFirst("td.statValue"))
                .map(Element::text)
                .orElse("未找到");
        //拼接数据后返回
        return Playername + "'s Bedwars Stats:Final K/D:" + finalKD + ",Doubles Final K/D:" + finalKD2v2 + ",4v4v4v4 Final K/D:" + finalKD4v4 + ",Total Wins:" + totalWins;
    }


    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("web")
                        .then(ClientCommandManager.argument("playername", StringArgumentType.string())
                                .suggests((context, builder) -> {

                                    if (Minecraft.getInstance().getConnection() != null) {
                                        String getInputPrefix = builder.getRemaining().toLowerCase();
                                        Minecraft.getInstance().getConnection().getListedOnlinePlayers().stream()
                                                .map(info -> info.getProfile().name())
                                                .filter(name -> name.toLowerCase().startsWith(getInputPrefix))
                                                .forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String player = StringArgumentType.getString(ctx, "playername");

                                    CompletableFuture
                                            .supplyAsync(() -> {
                                                try {
                                                    return extractBWStats(player);
                                                } catch (Exception e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }, IO_EXEC)
                                            .thenAcceptAsync(msg -> ctx.getSource().sendFeedback(
                                                    Component.literal(msg).withStyle(ChatFormatting.AQUA)), Minecraft.getInstance())
                                            .exceptionally(ex -> {
                                                Minecraft.getInstance().execute(() -> ctx.getSource().sendFeedback(
                                                        Component.literal("Failed: " + ex.getMessage())));
                                                return null;
                                            });

                                    return 1; // 立即返回，不阻塞服务器主线程
                                }))));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("weball")
                        .executes(ctx -> {

                            CompletableFuture
                                    .supplyAsync(() -> {
                                        if (Minecraft.getInstance().getConnection() != null) {
                                            Minecraft.getInstance().getConnection().getListedOnlinePlayers().stream()
                                                    .map(info -> info.getProfile().name())
                                                    .forEach(name -> {
                                                        try {
                                                            String stats = extractBWStats(name);
                                                            Minecraft.getInstance().execute(() -> ctx.getSource().sendFeedback(
                                                                    Component.literal(stats).withStyle(ChatFormatting.AQUA)));
                                                        } catch (Exception e) {
                                                            Minecraft.getInstance().execute(() -> ctx.getSource().sendFeedback(
                                                                    Component.literal("Failed to fetch stats for " + name + ": " + e.getMessage())));
                                                        }
                                                    });
                                        }
                                        return "Finished fetching stats for all online players.";
                                    }, IO_EXEC)
                                    .thenAcceptAsync(msg -> ctx.getSource().sendFeedback(
                                            Component.literal(msg).withStyle(ChatFormatting.AQUA)), Minecraft.getInstance())
                                    .exceptionally(ex -> {
                                        Minecraft.getInstance().execute(() -> ctx.getSource().sendFeedback(
                                                Component.literal("Failed: " + ex.getMessage())));
                                        return null;
                                    });

                            return 1; // 立即返回，不阻塞服务器主线程
                        })));
    }
}