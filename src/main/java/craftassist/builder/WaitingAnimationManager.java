package craftassist.builder;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WaitingAnimationManager {

    private static final String[] SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private static final int FRAME_INTERVAL = 4; // 每 4 tick 切換一幀

    private static final Map<UUID, WaitingTask> waitingTasks = new ConcurrentHashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (waitingTasks.isEmpty()) {
                return;
            }

            for (Map.Entry<UUID, WaitingTask> entry : waitingTasks.entrySet()) {
                UUID playerUuid = entry.getKey();
                WaitingTask task = entry.getValue();
                task.tickCount++;

                if (task.tickCount % FRAME_INTERVAL != 0) {
                    continue;
                }

                ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
                if (player != null) {
                    sendWaitingAnimation(player, task);
                }
            }
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
