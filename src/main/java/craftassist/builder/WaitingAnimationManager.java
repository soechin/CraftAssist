package craftassist.builder;

import craftassist.CraftAssistMod;
import craftassist.util.MessageUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WaitingAnimationManager {

    private static final String[] SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private static final int FRAME_INTERVAL = 4; // 每 4 tick 切換一幀

    private static final Map<UUID, WaitingTask> waitingTasks = new ConcurrentHashMap<>();

    private static final int TIMEOUT_TICKS = 120 * 20; // 120 秒

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (waitingTasks.isEmpty()) {
                return;
            }

            List<UUID> timeoutTasks = new ArrayList<>();

            for (Map.Entry<UUID, WaitingTask> entry : waitingTasks.entrySet()) {
                UUID playerUuid = entry.getKey();
                WaitingTask task = entry.getValue();
                task.tickCount++;

                // 超時檢查
                if (task.tickCount > TIMEOUT_TICKS) {
                    timeoutTasks.add(playerUuid);
                    ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
                    if (player != null) {
                        MessageUtil.sendError(player, "API 請求超時，請重試");
                    }
                    continue;
                }

                if (task.tickCount % FRAME_INTERVAL != 0) {
                    continue;
                }

                ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
                if (player != null) {
                    sendWaitingAnimation(player, task);
                }
            }

            timeoutTasks.forEach(waitingTasks::remove);
        });

        // 玩家斷線時清理等待狀態
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            waitingTasks.remove(handler.getPlayer().getUUID());
        });
    }

    public static void startWaiting(UUID playerUuid) {
        waitingTasks.put(playerUuid, new WaitingTask("生成中"));
    }

    public static void startWaiting(UUID playerUuid, String stage) {
        waitingTasks.put(playerUuid, new WaitingTask(stage));
    }

    public static void updateStage(UUID playerUuid, String stage) {
        WaitingTask task = waitingTasks.get(playerUuid);
        if (task != null) {
            task.stage = stage;
        }
    }

    public static void stopWaiting(UUID playerUuid) {
        waitingTasks.remove(playerUuid);
    }

    public static boolean isWaiting(UUID playerUuid) {
        return waitingTasks.containsKey(playerUuid);
    }

    public static void shutdown() {
        if (!waitingTasks.isEmpty()) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] 伺服器關閉時仍有 {} 個等待中任務", waitingTasks.size());
            waitingTasks.clear();
        }
    }

    private static void sendWaitingAnimation(ServerPlayer player, WaitingTask task) {
        int frameIndex = (task.tickCount / FRAME_INTERVAL) % SPINNER.length;
        int seconds = task.tickCount / 20;

        String text = "§6[CraftAssist] §e" + task.stage + " §f" + SPINNER[frameIndex] + " §7(" + seconds + "秒)";
        player.displayClientMessage(Component.literal(text), true);
    }

    private static class WaitingTask {
        int tickCount = 0;
        String stage;

        WaitingTask(String stage) {
            this.stage = stage;
        }
    }
}
