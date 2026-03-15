package org.exmple.webprofileviewer.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public class WeballCommand {
    public static final String CMD_WEBALL = "weball";
    // Rate limiter specific to /weball to prevent API throttling
    private static final RateLimiter RATE_LIMITER = new RateLimiter(5, 1.0);

    private static class FetchResult {
        final String summary;
        final java.util.List<PlayerKD> dangerous;

        FetchResult(String summary, java.util.List<PlayerKD> dangerous) {
            this.summary = summary;
            this.dangerous = dangerous;
        }
    }

    // small holder for a dangerous player's name and final KD string
    private static class PlayerKD {
        final String name;
        final String kd;

        PlayerKD(String name, String kd) {
            this.name = name;
            this.kd = kd;
        }
    }
    public static void register(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal(CMD_WEBALL)
                        .executes(ctx -> {

                            CompletableFuture
                                    .supplyAsync(() -> {
                                        // start timer for total elapsed time
                                        long startNano = System.nanoTime();

                                        java.util.List<PlayerKD> dangerous = new java.util.ArrayList<>();

                                        if (Minecraft.getInstance().getConnection() != null) {
                                            // collect cleaned player names first, then iterate sequentially
                                            String[] names = Minecraft.getInstance().getConnection().getListedOnlinePlayers().stream()
                                                    .map(info -> ServiceContainer.getNameFormatter().cleanPlayerName(info.getProfile().name()))
                                                    .toArray(String[]::new);
                                            int total = names.length;
                                            for (int idx = 0; idx < total; idx++) {
                                                final int current = idx + 1; // effectively final for lambda
                                                String name = names[idx];
                                                try {
                                                    String stats = ServiceContainer.getStatsExtractor().extractBWStats(name);

                                                    // Extract Final K/D value for danger detection
                                                    String kdValue = StatsFormatter.extractStatValue(stats, "Final K/D:");
                                                    if (kdValue != null) {
                                                        double kdVal = StatsFormatter.parseStatAsDouble(kdValue);
                                                        if (kdVal > 1.0) {
                                                            // store both name and KD string for later colored printing
                                                            dangerous.add(new PlayerKD(name, kdValue));
                                                        }
                                                    }

                                                    Minecraft.getInstance().execute(() -> {
                                                        // Show player name and [current/total] (bracket+colon in gold)
                                                        String headerStr = ChatFormatting.YELLOW + name + " " + ChatFormatting.GOLD + "[" + current + "/" + total + "]:";
                                                        ctx.getSource().sendFeedback(Component.literal(headerStr));

                                                        // Format and display stats with consistent coloring
                                                        Component[] formattedLines = StatsFormatter.formatStatsLines(stats);
                                                        for (Component line : formattedLines) {
                                                            ctx.getSource().sendFeedback(line);
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
                                    // Use token-bucket based rate limiter instead of fixed sleep.
                                    // This blocks only as long as necessary and allows short bursts.
                                    RATE_LIMITER.consume();
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    // If interrupted, stop further processing
                                    break;
                                }
                                            }
                                        }

                                        double elapsedSeconds = (System.nanoTime() - startNano) / 1_000_000_000.0;
                                        String summary = String.format("Finished fetching stats for all online players. Took %.3f seconds", elapsedSeconds);
                                        return new FetchResult(summary, dangerous);
                                    }, AsyncExecutor.getExecutor())
                                    .thenAcceptAsync(result -> {
                                        // print summary first
                                        ctx.getSource().sendFeedback(Component.literal(result.summary).withStyle(ChatFormatting.AQUA));

                                        // if any dangerous players found, print a red header and each name in red on its own line
                                        if (result.dangerous != null && !result.dangerous.isEmpty()) {
                                            ctx.getSource().sendFeedback(Component.literal("Dangerous Players:").withStyle(ChatFormatting.RED));
                                            for (PlayerKD pk : result.dangerous) {
                                                // name in red, KD in white within parentheses
                                                Component comp = Component.literal(pk.name).withStyle(ChatFormatting.RED)
                                                        .append(Component.literal(" (" + pk.kd + ")").withStyle(ChatFormatting.WHITE));
                                                ctx.getSource().sendFeedback(comp);
                                            }
                                        }
                                    }, Minecraft.getInstance())
                                    .exceptionally(ex -> {
                                        Minecraft.getInstance().execute(() -> ctx.getSource().sendFeedback(
                                                Component.literal("Failed: " + ex.getMessage())));
                                        return null;
                                    });

                            return 1; // 立即返回，不阻塞服务器主线程
                        })));
    }
}
