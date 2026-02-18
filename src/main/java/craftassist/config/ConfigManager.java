package craftassist.config;

import craftassist.CraftAssistMod;
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
                if (validateAndFix(config)) {
                    save();
                }
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

    static boolean validateAndFix(ModConfig cfg) {
        boolean fixed = false;

        if (cfg.getBlocksPerTick() <= 0) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] blocksPerTick 無效 ({})，已重設為 500", cfg.getBlocksPerTick());
            cfg.setBlocksPerTick(500);
            fixed = true;
        }

        if (cfg.getMaxBlocks() > 1_000_000) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] maxBlocks 過高 ({})，已限制為 1,000,000", cfg.getMaxBlocks());
            cfg.setMaxBlocks(1_000_000);
            fixed = true;
        }

        if (cfg.getTimeoutSeconds() < 10 || cfg.getTimeoutSeconds() > 300) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] timeoutSeconds 無效 ({})，已重設為 60", cfg.getTimeoutSeconds());
            cfg.setTimeoutSeconds(60);
            fixed = true;
        }

        if (cfg.getMaxRegionVolume() <= 0 || cfg.getMaxRegionVolume() > 1_000_000) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] maxRegionVolume 無效 ({})，已重設為 100,000", cfg.getMaxRegionVolume());
            cfg.setMaxRegionVolume(100_000);
            fixed = true;
        }

        if (cfg.getMaxCoordinate() <= 0 || cfg.getMaxCoordinate() > 1_000) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] maxCoordinate 無效 ({})，已重設為 200", cfg.getMaxCoordinate());
            cfg.setMaxCoordinate(200);
            fixed = true;
        }

        if (cfg.getRateLimitTokens() <= 0) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] rateLimitTokens 無效 ({})，已重設為 3", cfg.getRateLimitTokens());
            cfg.setRateLimitTokens(3);
            fixed = true;
        }

        if (cfg.getRateLimitRefillSeconds() <= 0 || cfg.getRateLimitRefillSeconds() > 3600) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] rateLimitRefillSeconds 無效 ({})，已重設為 60", cfg.getRateLimitRefillSeconds());
            cfg.setRateLimitRefillSeconds(60);
            fixed = true;
        }

        if (cfg.getMaxRetries() < 0 || cfg.getMaxRetries() > 10) {
            CraftAssistMod.LOGGER.warn("[CraftAssist] maxRetries 無效 ({})，已重設為 2", cfg.getMaxRetries());
            cfg.setMaxRetries(2);
            fixed = true;
        }

        return fixed;
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
