package craftassist.builder;

import craftassist.CraftAssistMod;
import craftassist.config.ConfigManager;
import craftassist.undo.UndoData;
import craftassist.undo.UndoManager;
import craftassist.util.MessageUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BatchPlacementManager {

    private static final Map<UUID, BatchTask> activeTasks = new ConcurrentHashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (activeTasks.isEmpty()) {
                return;
            }

            int blocksPerTick = ConfigManager.getConfig().getBlocksPerTick();
            List<UUID> completedTasks = new ArrayList<>();

            for (Map.Entry<UUID, BatchTask> entry : activeTasks.entrySet()) {
                UUID playerUuid = entry.getKey();
                BatchTask task = entry.getValue();

                boolean done = task.processBatch(blocksPerTick);

                ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
                if (player != null) {
                    sendProgressBar(player, task);
                }

                if (done) {
                    completedTasks.add(playerUuid);
                    onTaskComplete(playerUuid, task, player);
                }
            }

            completedTasks.forEach(activeTasks::remove);
        });
    }

    public static boolean startTask(UUID playerUuid, BatchTask task) {
        return activeTasks.putIfAbsent(playerUuid, task) == null;
    }

    public static boolean hasActiveTask(UUID playerUuid) {
        return activeTasks.containsKey(playerUuid);
    }

    public static void shutdown() {
        if (!activeTasks.isEmpty()) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] 伺服器關閉時仍有 {} 個未完成的批次任務", activeTasks.size());
            activeTasks.clear();
        }
    }

    private static void onTaskComplete(UUID playerUuid, BatchTask task, ServerPlayer player) {
        if (task.isUndoTask()) {
            CraftAssistMod.LOGGER.info("[CraftAssist] 復原了 {} 個方塊", task.getTotalBlocks());
            if (player != null) {
                MessageUtil.sendSuccess(player, "已復原 " + task.getTotalBlocks() + " 個方塊");
            }
        } else {
            // 僅 build 任務記錄 undo 資料
            UndoManager.recordUndo(playerUuid, task.getUndoData());
            CraftAssistMod.LOGGER.info("[CraftAssist] 放置了 {} 個方塊", task.getTotalBlocks());
            if (player != null) {
                MessageUtil.sendSuccess(player, "完成！放置了 " + task.getTotalBlocks() + " 個方塊");
            }
        }
    }

    private static void sendProgressBar(ServerPlayer player, BatchTask task) {
        int percent = task.getProgressPercent();
        // 僅在百分比變化時更新
        if (percent == task.getLastReportedPercent()) {
            return;
        }
        task.setLastReportedPercent(percent);

        int barLen = 20;
        int filled = percent * barLen / 100;

        String label = task.isUndoTask() ? "復原中" : "建築中";
        StringBuilder bar = new StringBuilder("§6[CraftAssist] " + label + " §a[");
        for (int i = 0; i < barLen; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("§a] §e").append(percent).append("% §7(")
                .append(task.getCurrentIndex()).append("/").append(task.getTotalBlocks()).append(")");

        player.displayClientMessage(Component.literal(bar.toString()), true);
    }

    // ========== 批次任務資料類別 ==========

    public static class BatchTask {
        private final ServerLevel world;
        private final List<BlockPlacement> placements;
        private final UndoData undoData;
        private final Set<BlockPos> recordedPositions;
        private final boolean undoTask;
        private int currentIndex = 0;
        private int lastReportedPercent = -1;

        public BatchTask(ServerLevel world, List<BlockPlacement> placements) {
            this(world, placements, false);
        }

        public BatchTask(ServerLevel world, List<BlockPlacement> placements, boolean undoTask) {
            this.world = world;
            this.placements = placements;
            this.undoData = new UndoData();
            this.recordedPositions = new HashSet<>();
            this.undoTask = undoTask;
        }

        public boolean isUndoTask() {
            return undoTask;
        }

        /**
         * 處理一批方塊。
         *
         * @return true 如果全部完成
         */
        public boolean processBatch(int batchSize) {
            int end = Math.min(currentIndex + batchSize, placements.size());

            for (int i = currentIndex; i < end; i++) {
                BlockPlacement bp = placements.get(i);
                // 同一位置只記錄第一次的原始狀態，避免重疊 region 覆蓋真正的原始方塊
                if (recordedPositions.add(bp.pos())) {
                    BlockState original = world.getBlockState(bp.pos());
                    undoData.addSnapshot(bp.pos(), original);
                }
                world.setBlockAndUpdate(bp.pos(), bp.state());
            }

            currentIndex = end;
            return currentIndex >= placements.size();
        }

        public int getProgressPercent() {
            if (placements.isEmpty()) return 100;
            return (int) ((long) currentIndex * 100 / placements.size());
        }

        public int getCurrentIndex() {
            return currentIndex;
        }

        public int getTotalBlocks() {
            return placements.size();
        }

        public UndoData getUndoData() {
            return undoData;
        }

        public int getLastReportedPercent() {
            return lastReportedPercent;
        }

        public void setLastReportedPercent(int percent) {
            this.lastReportedPercent = percent;
        }
    }

    public record BlockPlacement(BlockPos pos, BlockState state) {
    }
}
