package com.craftassist;

import com.craftassist.builder.BatchPlacementManager;
import com.craftassist.builder.WaitingAnimationManager;
import com.craftassist.command.CraftAssistCommand;
import com.craftassist.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
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
        LOGGER.info("[CraftAssist] 模組已載入");
    }
}
