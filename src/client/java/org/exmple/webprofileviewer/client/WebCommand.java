package org.exmple.webprofileviewer.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public class WebCommand {
    public static final String CMD_WEB="web";
    public static void register(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal(CMD_WEB)
                        .executes(ctx -> {
                            // When user types only `/web` without args, show a red usage message
                            ctx.getSource().sendFeedback(
                                    Component.literal("Correct usage:/web <Username>").withStyle(ChatFormatting.RED)
                            );
                            return 1;
                        })
                        .then(ClientCommandManager.argument("playername", StringArgumentType.string())
                                .suggests((context, builder) -> {

                                    if (Minecraft.getInstance().getConnection() != null) {
                                        String getInputPrefix = builder.getRemaining().toLowerCase();
                                        Minecraft.getInstance().getConnection().getListedOnlinePlayers().stream()
                                                .map(info -> {String rawName=info.getProfile().name();
                                                    return ServiceContainer.getNameFormatter().cleanPlayerName(rawName) ;})
                                                .filter(name -> name.toLowerCase().startsWith(getInputPrefix))
                                                .forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String player = StringArgumentType.getString(ctx, "playername");
                                    String cleanPlayer = ServiceContainer.getNameFormatter().cleanPlayerName(player);
                                    CompletableFuture
                                            .supplyAsync(() -> {
                                                try {
                                                    return ServiceContainer.getStatsExtractor().extractBWStats(cleanPlayer);
                                                } catch (Exception e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }, AsyncExecutor.getExecutor())
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
    }
}
