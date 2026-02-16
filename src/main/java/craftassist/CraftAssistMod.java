package craftassist;

import craftassist.builder.BatchPlacementManager;
import craftassist.builder.WaitingAnimationManager;
import craftassist.command.CraftAssistCommand;
import craftassist.config.ConfigManager;
import craftassist.undo.UndoManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CraftAssistMod implements ModInitializer {

    public static final String MOD_ID = "craftassist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ConfigManager.load();
        CraftAssistCommand.register();
        BatchPlacementManager.init();
        WaitingAnimationManager.init();
        UndoManager.init();

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("[CraftAssist] 伺服器關閉中，清理資源...");
            BatchPlacementManager.shutdown();
            WaitingAnimationManager.shutdown();
        });

        LOGGER.info("[CraftAssist] 模組已載入");
    }
}
