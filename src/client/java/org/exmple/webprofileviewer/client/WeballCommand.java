package org.exmple.webprofileviewer.client;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class WeballCommand {
    public static final String CMD_WEBALL = "weball";
    private static final Logger LOGGER = LoggerFactory.getLogger("webprofileviewer");
    private static final RateLimiter RATE_LIMITER = new RateLimiter(5, 1.0);

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal(CMD_WEBALL)
                        .executes(WeballCommand::execute)));
    }

    private static int execute(CommandContext<FabricClientCommandSource> ctx) {
        if (Minecraft.getInstance().getConnection() == null) return 0;

        // Capture player list on main thread
        List<String> names = Minecraft.getInstance().getConnection().getListedOnlinePlayers().stream()
                .map(info -> ServiceContainer.getNameFormatter().cleanPlayerName(info.getProfile().name()))
                .collect(Collectors.toList());

        CompletableFuture.supplyAsync(() -> processPlayers(ctx, names), AsyncExecutor.getExecutor())
                .thenAcceptAsync(result -> displaySummary(ctx, result), Minecraft.getInstance())
                .exceptionally(ex -> handleError(ctx, ex));

        return 1;
    }

    private static FetchResult processPlayers(CommandContext<FabricClientCommandSource> ctx, List<String> names) {
        long startNano = System.nanoTime();
        List<PlayerKD> dangerous = new ArrayList<>();
        int total = names.size();

        for (int i = 0; i < total; i++) {
            String name = names.get(i);
            int current = i + 1;

            try {
                processSinglePlayer(ctx, name, current, total, dangerous);
            } catch (Exception e) {
                handlePlayerError(ctx, name, current, total, dangerous);
            }

            // Rate limiter
            try {
                RATE_LIMITER.consume();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        double elapsedSeconds = (System.nanoTime() - startNano) / 1_000_000_000.0;
        String summary = String.format("Finished fetching stats for all online players. Took %.3f seconds", elapsedSeconds);
        return new FetchResult(summary, dangerous);
    }

    private static void processSinglePlayer(CommandContext<FabricClientCommandSource> ctx, String name, int current, int total, List<PlayerKD> dangerous) throws Exception {
        BWStatsExtractor.BWStats stats = ServiceContainer.getStatsExtractor().extractBWStats(name);

        // Detect dangerous player
        String kdValue = stats.getFinalKD();
        if (kdValue != null && !"未找到".equals(kdValue)) {
            double kdVal = StatsFormatter.parseStatAsDouble(kdValue);
            if (kdVal > 1.0) {
                dangerous.add(new PlayerKD(name, kdValue));
            }
        }

        // Update UI
        Minecraft.getInstance().execute(() -> {
            String headerStr = ChatFormatting.YELLOW + name + " " + ChatFormatting.GOLD + "[" + current + "/" + total + "]:";
            ctx.getSource().sendFeedback(Component.literal(headerStr));

            for (Component line : StatsFormatter.formatStats(stats)) {
                ctx.getSource().sendFeedback(line);
            }
        });
    }

    private static void handlePlayerError(CommandContext<FabricClientCommandSource> ctx, String name, int current, int total, List<PlayerKD> dangerous) {
        dangerous.add(new PlayerKD(name, "nicked"));
        Minecraft.getInstance().execute(() -> {
            String failMsg = ChatFormatting.YELLOW + name + " " + ChatFormatting.GOLD + "[" + current + "/" + total + "]:" + ChatFormatting.RED + "\nThis player may be nicked!";
            ctx.getSource().sendFeedback(Component.literal(failMsg));
        });
    }

    private static void displaySummary(CommandContext<FabricClientCommandSource> ctx, FetchResult result) {
        ctx.getSource().sendFeedback(Component.literal(result.summary).withStyle(ChatFormatting.AQUA));

        if (result.dangerous != null && !result.dangerous.isEmpty()) {
            ctx.getSource().sendFeedback(Component.literal("Dangerous Players:").withStyle(ChatFormatting.RED));
            for (PlayerKD pk : result.dangerous) {
                Component comp = Component.literal(pk.name).withStyle(ChatFormatting.RED)
                        .append(Component.literal(" (" + pk.kd + ")").withStyle(ChatFormatting.WHITE));
                ctx.getSource().sendFeedback(comp);
            }
        }
    }

    private static Void handleError(CommandContext<FabricClientCommandSource> ctx, Throwable ex) {
        LOGGER.error("Weball execution failed", ex);
        Minecraft.getInstance().execute(() -> ctx.getSource().sendFeedback(
                Component.literal("Failed: " + ex.getMessage())));
        return null;
    }

    private record FetchResult(String summary, List<PlayerKD> dangerous) {}
    private record PlayerKD(String name, String kd) {}
}
