package com.craftassist.undo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UndoManager {

    private static final Map<UUID, UndoData> playerHistory = new ConcurrentHashMap<>();

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
