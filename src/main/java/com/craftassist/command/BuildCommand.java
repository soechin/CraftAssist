package com.craftassist.command;

import com.craftassist.api.OpenRouterClient;
import com.craftassist.builder.BlockPlacementEngine;
import com.craftassist.config.ConfigManager;
import com.craftassist.config.ModConfig;
import com.craftassist.util.MessageUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class BuildCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("build")
                    .requires(source -> source.permissions().hasPermission(
                            net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("description", StringArgumentType.greedyString())
                            .executes(context -> {
                                String description = StringArgumentType.getString(context, "description");
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                ServerLevel world = context.getSource().getLevel();
                                MinecraftServer server = context.getSource().getServer();

                                return execute(player, world, server, description);
                            })));
        });
    }

    private static int execute(ServerPlayer player, ServerLevel world,
                               MinecraftServer server, String description) {
        ModConfig config = ConfigManager.getConfig();

        if (config.getApiKey().isEmpty()) {
            MessageUtil.sendError(player, "請先在 config/craftassist.json 中設定 API Key");
            return 0;
        }

        BlockPos origin = player.blockPosition().above();
        MessageUtil.sendProgress(player, "正在生成建築: " + description + " ...");

        OpenRouterClient.generate(description, config)
                .thenAccept(structure -> {
                    server.execute(() -> {
                        if (structure == null) {
                            MessageUtil.sendError(player, "生成失敗：無法解析 AI 回應");
                            return;
                        }
                        int placed = BlockPlacementEngine.place(world, origin, structure, config.getMaxBlocks());
                        if (placed < 0) {
                            int estimated = -placed;
                            MessageUtil.sendError(player, "建築需要 " + estimated + " 個方塊，超過限制（" + config.getMaxBlocks() + "）");
                        } else if (placed == 0) {
                            MessageUtil.sendError(player, "建築結構為空，沒有方塊可放置");
                        } else {
                            MessageUtil.sendSuccess(player, "完成！放置了 " + placed + " 個方塊");
                        }
                    });
                })
                .exceptionally(ex -> {
                    server.execute(() -> {
                        MessageUtil.sendError(player, "生成失敗: " + ex.getMessage());
                    });
                    return null;
                });

        return Command.SINGLE_SUCCESS;
    }
}
