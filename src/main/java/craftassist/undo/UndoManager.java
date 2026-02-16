package craftassist.undo;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UndoManager {

    private static final Map<UUID, UndoData> playerHistory = new ConcurrentHashMap<>();

    public static void init() {
        // 玩家斷線時清理 undo 資料，避免記憶體洩漏
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            playerHistory.remove(handler.getPlayer().getUUID());
        });
    }

    public static void recordUndo(UUID playerUuid, UndoData undoData) {
        playerHistory.put(playerUuid, undoData);
    }

    public static UndoData popUndo(UUID playerUuid) {
        return playerHistory.remove(playerUuid);
    }

    public static boolean hasUndo(UUID playerUuid) {
        return playerHistory.containsKey(playerUuid);
    }
}
