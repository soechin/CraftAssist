package com.craftassist.api;

import com.craftassist.CraftAssistMod;
import com.craftassist.builder.BuildStructure;
import com.craftassist.config.ModConfig;
import com.google.gson.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class OpenRouterClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final Gson GSON = new Gson();

    /**
     * 第一階段：創意規劃（回傳純文字藍圖）
     */
    public static CompletableFuture<String> generatePlan(String description, ModConfig config) {
        String systemPrompt = PromptBuilder.buildPlanningPrompt();

        String userPrompt = "Build: " + description;

        CraftAssistMod.LOGGER.debug("[CraftAssist] 第一階段 API 請求 | 模型: {} | 描述: {}", config.getModel(), description);

        return sendRequest(systemPrompt, userPrompt, config, false, "第一階段")
                .thenApply(body -> parseTextResponse(body, "第一階段"));
    }

    /**
     * 第二階段：完整建築生成（回傳建築 JSON）
     */
    public static CompletableFuture<BuildStructure> generateBuilding(String blueprint, ModConfig config) {
        String systemPrompt = PromptBuilder.buildBuildingPrompt(blueprint, config.getMaxBlocks());

        String userPrompt = "Generate the complete building JSON following the blueprint above.";

        CraftAssistMod.LOGGER.debug("[CraftAssist] 第二階段 API 請求 | 模型: {}", config.getModel());

        return sendRequest(systemPrompt, userPrompt, config, true, "第二階段")
                .thenApply(body -> parseBuildResponse(body, "第二階段"));
    }

    private static CompletableFuture<String> sendRequest(String systemPrompt, String userPrompt,
                                                         ModConfig config, boolean jsonFormat, String stage) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getModel());

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        if (jsonFormat) {
            JsonObject responseFormat = new JsonObject();
            responseFormat.addProperty("type", "json_object");
            requestBody.add("response_format", responseFormat);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .header("X-Title", "CraftAssist Minecraft Mod")
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .build();

        long startTime = System.nanoTime();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
                    if (response.statusCode() != 200) {
                        CraftAssistMod.LOGGER.error("[CraftAssist] {} API 錯誤 | HTTP {} | 耗時: {}ms | 回應: {}",
                                stage, response.statusCode(), elapsedMs, response.body());
                        throw new RuntimeException("API 回應錯誤 (HTTP " + response.statusCode() + "): " + response.body());
                    }
                    CraftAssistMod.LOGGER.debug("[CraftAssist] {} API 回應 | HTTP {} | 耗時: {}ms",
                            stage, response.statusCode(), elapsedMs);
                    return response.body();
                });
    }

    private static String parseTextResponse(String responseBody, String stage) {
        try {
            JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
            String content = response.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            CraftAssistMod.LOGGER.debug("[CraftAssist] {} 藍圖內容:\n{}", stage, content);

            return content;
        } catch (Exception e) {
            CraftAssistMod.LOGGER.error("[CraftAssist] {} 無法解析 API 回應", stage, e);
            return null;
        }
    }

    private static BuildStructure parseBuildResponse(String responseBody, String stage) {
        try {
            JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
            String content = response.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            CraftAssistMod.LOGGER.debug("[CraftAssist] {} LLM 回應內容:\n{}", stage, content);

            return GSON.fromJson(content, BuildStructure.class);
        } catch (Exception e) {
            CraftAssistMod.LOGGER.error("[CraftAssist] {} 無法解析 API 回應", stage, e);
            return null;
        }
    }
}
