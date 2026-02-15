package com.craftassist.config;

import com.craftassist.CraftAssistMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig config = new ModConfig();

    public static void load() {
        Path configPath = getConfigPath();
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                config = GSON.fromJson(json, ModConfig.class);
                CraftAssistMod.LOGGER.info("[CraftAssist] 配置已載入");
            } catch (IOException e) {
                CraftAssistMod.LOGGER.error("[CraftAssist] 無法讀取配置檔案", e);
                config = new ModConfig();
            }
        } else {
            save();
            CraftAssistMod.LOGGER.info("[CraftAssist] 已建立預設配置檔案: {}", configPath);
        }
    }

    public static void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(config));
        } catch (IOException e) {
            CraftAssistMod.LOGGER.error("[CraftAssist] 無法儲存配置檔案", e);
        }
    }

    public static ModConfig getConfig() {
        return config;
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("craftassist.json");
    }
}
