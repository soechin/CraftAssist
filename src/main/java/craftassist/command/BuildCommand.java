package craftassist.command;

import craftassist.api.ApiException;
import craftassist.api.OpenRouterClient;
import craftassist.api.RateLimiter;
import craftassist.builder.BatchPlacementManager;
import craftassist.builder.BlockPlacementEngine;
import craftassist.builder.BuildStructureRotator;
import craftassist.builder.BuildingOffsetCalculator;
import craftassist.builder.WaitingAnimationManager;
import craftassist.config.ConfigManager;
import craftassist.config.ModConfig;
import craftassist.util.MessageUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class BuildCommand {

    public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String description = StringArgumentType.getString(context, "description");
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel world = context.getSource().getLevel();
        MinecraftServer server = context.getSource().getServer();

        return executeBuild(player, world, server, description);
    }

    private static int executeBuild(ServerPlayer player, ServerLevel world,
                                    MinecraftServer server, String description) {
        ModConfig config = ConfigManager.getConfig();
        UUID playerUuid = player.getUUID();

        if (config.getApiKey().isEmpty()) {
            MessageUtil.sendError(player, "請先在 config/craftassist.json 中設定 API Key");
            return 0;
        }

        // 檢查是否有進行中的任務
        if (BatchPlacementManager.hasActiveTask(playerUuid) || WaitingAnimationManager.isWaiting(playerUuid)) {
            MessageUtil.sendError(player, "您已有進行中的建築任務，請稍後再試");
            return 0;
        }

        // 速率限制
        if (!RateLimiter.tryConsume(playerUuid)) {
            MessageUtil.sendError(player, "請求過於頻繁，請稍後再試");
            return 0;
        }

        BlockPos playerPos = player.blockPosition();
        Direction facing = player.getDirection();
        WaitingAnimationManager.startWaiting(playerUuid, "階段 1/2：設計規劃");

        // 第一階段：創意規劃
        OpenRouterClient.generatePlan(description, facing, config)
                .thenCompose(blueprint -> {
                    if (blueprint == null || blueprint.isBlank()) {
                        throw new RuntimeException("設計規劃失敗：無法取得建築藍圖");
                    }

                    // 更新進度提示
                    server.execute(() ->
                            WaitingAnimationManager.updateStage(playerUuid, "階段 2/2：生成建築"));

                    // 第二階段：完整建築生成
                    return OpenRouterClient.generateBuilding(blueprint, config);
                })
                .thenAccept(structure -> {
                    server.execute(() -> {
                        WaitingAnimationManager.stopWaiting(playerUuid);

                        if (structure == null) {
                            MessageUtil.sendError(player, "生成失敗：無法解析 AI 回應");
                            return;
                        }

                        // 偵測入口牆面，必要時旋轉建築使門面對玩家
                        Direction entranceWall =
                                BuildStructureRotator.detectEntranceWall(structure);
                        if (entranceWall != null) {
                            int rotations = BuildStructureRotator.computeRotationCount(
                                    entranceWall, facing);
                            if (rotations != 0) {
                                BuildStructureRotator.rotateStructure(structure, rotations);
                            }
                        }

                        // 旋轉後重新計算 bbox 和 origin，使建築出現在玩家前方
                        BuildingOffsetCalculator.BoundingBox bbox =
                                BuildingOffsetCalculator.computeBoundingBox(structure);
                        BlockPos origin = BuildingOffsetCalculator.computeOrigin(
                                playerPos, facing, bbox);

                        // 預計算所有方塊放置清單
                        List<BatchPlacementManager.BlockPlacement> placements =
                                BlockPlacementEngine.preparePlacements(origin, structure);

                        if (placements.isEmpty()) {
                            MessageUtil.sendError(player, "建築結構為空，沒有方塊可放置");
                            return;
                        }

                        // 啟動批次放置任務
                        BatchPlacementManager.BatchTask task =
                                new BatchPlacementManager.BatchTask(world, placements);

                        if (!BatchPlacementManager.startTask(playerUuid, task)) {
                            MessageUtil.sendError(player, "您已有進行中的建築任務，請稍後再試");
                            return;
                        }

                        MessageUtil.sendProgress(player, "開始放置 " + placements.size() + " 個方塊...");
                    });
                })
                .exceptionally(ex -> {
                    server.execute(() -> {
                        WaitingAnimationManager.stopWaiting(playerUuid);
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        if (cause instanceof ApiException apiEx) {
                            MessageUtil.sendError(player, apiEx.getMessage());
                        } else {
                            MessageUtil.sendError(player, "生成失敗：" + cause.getMessage());
                        }
                    });
                    return null;
                });

        return Command.SINGLE_SUCCESS;
    }
}
