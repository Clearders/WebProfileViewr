package org.exmple.webprofileviewer.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class WebCommand {
    public static final String CMD_WEB = "web";
    private static final Logger LOGGER = LoggerFactory.getLogger("webprofileviewer");

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal(CMD_WEB)
                        .executes(WebCommand::showUsage)
                        .then(ClientCommandManager.argument("playername", StringArgumentType.string())
                                .suggests(WebCommand::suggestPlayerNames)
                                .executes(WebCommand::fetchAndDisplayStats))));
    }

    private static int showUsage(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(
                Component.literal("Correct usage: /web <Username>").withStyle(ChatFormatting.RED)
        );
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestPlayerNames(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
        if (Minecraft.getInstance().getConnection() == null) return builder.buildFuture();

        String inputPrefix = builder.getRemaining().toLowerCase();
        Minecraft.getInstance().getConnection().getListedOnlinePlayers().stream()
                .map(info -> info.getProfile().name())
                .map(name -> ServiceContainer.getNameFormatter().cleanPlayerName(name))
                .filter(name -> name.toLowerCase().startsWith(inputPrefix))
                .forEach(builder::suggest);

        return builder.buildFuture();
    }

    private static int fetchAndDisplayStats(CommandContext<FabricClientCommandSource> ctx) {
        String player = StringArgumentType.getString(ctx, "playername");
        String cleanPlayer = ServiceContainer.getNameFormatter().cleanPlayerName(player);

        CompletableFuture.supplyAsync(() -> fetchStats(cleanPlayer), AsyncExecutor.getExecutor())
                .thenAcceptAsync(stats -> displayStats(ctx, cleanPlayer, stats), Minecraft.getInstance())
                .exceptionally(ex -> handleFetchError(ctx, cleanPlayer, ex));

        return 1;
    }

    private static BWStatsExtractor.BWStats fetchStats(String player) {
        try {
            return ServiceContainer.getStatsExtractor().extractBWStats(player);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void displayStats(CommandContext<FabricClientCommandSource> ctx, String player, BWStatsExtractor.BWStats stats) {
        ctx.getSource().sendFeedback(Component.literal(player + ":").withStyle(ChatFormatting.YELLOW));
        for (Component line : StatsFormatter.formatStats(stats)) {
            ctx.getSource().sendFeedback(line);
        }
    }

    private static Void handleFetchError(CommandContext<FabricClientCommandSource> ctx, String player, Throwable ex) {
        LOGGER.error("Failed to fetch stats for {}", player, ex);
        Minecraft.getInstance().execute(() -> {
            String failMsg = ChatFormatting.YELLOW + player + " :\n" + ChatFormatting.RED + "This player may be nicked!";
            ctx.getSource().sendFeedback(Component.literal(failMsg));
        });
        return null;
    }
}
