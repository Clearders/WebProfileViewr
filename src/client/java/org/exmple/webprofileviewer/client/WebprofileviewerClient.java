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
        thread.setName("WebProfileViewer-IO");
        return thread;
    });

    private String cleanPlayerName(String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return "";
        }
        // StripFormatting的作用:一键移除所有§开头的格式符（如§r等）
        return ChatFormatting.stripFormatting(rawName);
    }// 方法作用：移除玩家名字中的格式符，确保后续处理时使用干净的名字

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
        String stats="Final K/D:" + finalKD + "\nDoubles Final K/D:" + finalKD2v2 + "\n4v4v4v4 Final K/D:" + finalKD4v4 + "\nTotal Wins:" + totalWins;
        return stats;
    }


    @Override
    public void onInitializeClient() {

        String cmd = "web";



                ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                        dispatcher.register(ClientCommandManager.literal(cmd)
                                .then(ClientCommandManager.argument("playername", StringArgumentType.string())
                                        .suggests((context, builder) -> {

                                            if (Minecraft.getInstance().getConnection() != null) {
                                                String getInputPrefix = builder.getRemaining().toLowerCase();
                                                Minecraft.getInstance().getConnection().getListedOnlinePlayers().stream()
                                                        .map(info -> {String rawName=info.getProfile().name();
                                                            return cleanPlayerName(rawName);})
                                                        .filter(name -> name.toLowerCase().startsWith(getInputPrefix))
                                                        .forEach(builder::suggest);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String player = StringArgumentType.getString(ctx, "playername");
                                            String cleanPlayer = cleanPlayerName(player);
                                            CompletableFuture
                                                    .supplyAsync(() -> {
                                                        try {
                                                            return extractBWStats(cleanPlayer);
                                                        } catch (Exception e) {
                                                            throw new RuntimeException(e);
                                                        }
                                                    }, IO_EXEC)
                                                    .thenAcceptAsync(msg -> Minecraft.getInstance().execute(() -> {
                                                        // Show player name in yellow
                                                        Component header = Component.literal(cleanPlayer + ":").withStyle(ChatFormatting.YELLOW);
                                                        ctx.getSource().sendFeedback(header);

                                                        // Split stats into lines and colorize label vs value
                                                        String[] lines = msg.split("\\r?\\n");//根据换行符分割成多行
                                                        for (String line : lines) {
                                                            if (line.contains(":")) {
                                                                int colonIdx = line.indexOf(":");
                                                                String label = line.substring(0, colonIdx).trim();
                                                                String value = line.substring(colonIdx + 1).trim();
                                                                String colored = ChatFormatting.AQUA + label + ": " + ChatFormatting.WHITE + value;
                                                                ctx.getSource().sendFeedback(Component.literal(colored));
                                                            } else {
                                                                ctx.getSource().sendFeedback(Component.literal(ChatFormatting.WHITE + line));
                                                            }
                                                        }
                                                    }), Minecraft.getInstance())
                                                    .exceptionally(ex -> {
                                                        Minecraft.getInstance().execute(() -> {
                                                            // Show username in yellow, then a red friendly message
                                                            String failMsg = ChatFormatting.YELLOW + cleanPlayer + " :\n" +  ChatFormatting.RED + "This player may be nicked!";
                                                            ctx.getSource().sendFeedback(Component.literal(failMsg));
                                                        });
                                                        return null;
                                                    });

                                            return 1; // 立即返回，不阻塞服务器主线程
                                        }))));



        String cmds = "weball";




                ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                        dispatcher.register(ClientCommandManager.literal(cmds)
                                .executes(ctx -> {

                                    CompletableFuture
                                            .supplyAsync(() -> {
                                                if (Minecraft.getInstance().getConnection() != null) {
                                                    // collect cleaned player names first, then iterate sequentially
                                                    String[] names = Minecraft.getInstance().getConnection().getListedOnlinePlayers().stream()
                                                            .map(info -> cleanPlayerName(info.getProfile().name()))
                                                            .toArray(String[]::new);
                                                    int total = names.length;
                                                    for (int idx = 0; idx < total; idx++) {
                                                        final int current = idx + 1; // effectively final for lambda
                                                        String name = names[idx];
                                                         try {
                                                             String stats = extractBWStats(name);
                                                             Minecraft.getInstance().execute(() -> {
                                                                 // Show player name and [current/total] (bracket+colon in gold)
                                                                 String headerStr = ChatFormatting.YELLOW + name + " " + ChatFormatting.GOLD + "[" + current + "/" + total + "]:";
                                                                 ctx.getSource().sendFeedback(Component.literal(headerStr));

                                                                 // Split stats into lines and colorize label vs value
                                                                 String[] lines = stats.split("\\r?\\n");
                                                                 for (String line : lines) {
                                                                     if (line.contains(":")) {
                                                                         int colonIdx = line.indexOf(":");
                                                                         String label = line.substring(0, colonIdx).trim(); // label without ':'
                                                                         String value = line.substring(colonIdx + 1).trim();
                                                                         // color label in AQUA, then a space, then value in WHITE
                                                                         String colored = ChatFormatting.AQUA + label + ": " + ChatFormatting.WHITE + value;
                                                                         ctx.getSource().sendFeedback(Component.literal(colored));
                                                                     } else {
                                                                         ctx.getSource().sendFeedback(Component.literal(ChatFormatting.WHITE + line));
                                                                     }
                                                                 }
                                                             });
                                                         } catch (Exception e) {
                                                             // On failure, show a user-friendly, colored message similar to /web
                                                             Minecraft.getInstance().execute(() -> {
                                                                 String failMsg = ChatFormatting.YELLOW + name + " " + ChatFormatting.GOLD + "[" + current + "/" + total + "]:" + ChatFormatting.RED + "\nThis player may be nicked!";
                                                                 ctx.getSource().sendFeedback(Component.literal(failMsg));
                                                             });
                                                         }
                                                         //设置延迟，防止触发Hypixel的反爬虫机制
                                                         try {
                                                             Thread.sleep(500);
                                                         } catch (InterruptedException ie) {
                                                             Thread.currentThread().interrupt();
                                                             // If interrupted, stop further processing
                                                             break;
                                                         }
                                                    }
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
            }//onInitializeClient的大括号



    }//WebprofileviewerClient类的大括号
