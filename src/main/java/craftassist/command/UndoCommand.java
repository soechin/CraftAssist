package craftassist.command;

import craftassist.builder.BatchPlacementManager;
import craftassist.undo.UndoData;
import craftassist.undo.UndoManager;
import craftassist.util.MessageUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UndoCommand {

    public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerUuid = player.getUUID();

        // 有進行中的批次任務時拒絕
        if (BatchPlacementManager.hasActiveTask(playerUuid)) {
            MessageUtil.sendError(player, "請等待當前建築任務完成後再執行復原");
            return 0;
        }

        UndoData undoData = UndoManager.popUndo(playerUuid);
        if (undoData == null) {
            MessageUtil.sendError(player, "沒有可復原的建築操作");
            return 0;
        }

        ServerLevel world = context.getSource().getLevel();

        // 大量方塊也走批次還原
        List<UndoData.BlockSnapshot> snapshots = undoData.getSnapshots();
        List<BatchPlacementManager.BlockPlacement> restorations = new ArrayList<>(snapshots.size());

        for (UndoData.BlockSnapshot snapshot : snapshots) {
            restorations.add(new BatchPlacementManager.BlockPlacement(snapshot.pos(), snapshot.originalState()));
        }

        BatchPlacementManager.BatchTask undoTask = new BatchPlacementManager.BatchTask(world, restorations, true);

        if (!BatchPlacementManager.startTask(playerUuid, undoTask)) {
            MessageUtil.sendError(player, "無法開始復原任務");
            return 0;
        }

        MessageUtil.sendProgress(player, "開始復原 " + snapshots.size() + " 個方塊...");
        return Command.SINGLE_SUCCESS;
    }
}
