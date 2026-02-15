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

    public static CompletableFuture<BuildStructure> generate(String description, ModConfig config) {
        String systemPrompt = PromptBuilder.buildSystemPrompt(config.getMaxBlocks());

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getModel());

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", "Build: " + description);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .header("X-Title", "CraftAssist Minecraft Mod")
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .build();

        CraftAssistMod.LOGGER.debug("[CraftAssist] API 請求 | 模型: {} | 描述: {}", config.getModel(), description);
        long startTime = System.nanoTime();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
                    if (response.statusCode() != 200) {
                        CraftAssistMod.LOGGER.error("[CraftAssist] API 錯誤 | HTTP {} | 耗時: {}ms | 回應: {}",
                                response.statusCode(), elapsedMs, response.body());
                        throw new RuntimeException("API 回應錯誤 (HTTP " + response.statusCode() + "): " + response.body());
                    }
                    CraftAssistMod.LOGGER.debug("[CraftAssist] API 回應 | HTTP {} | 耗時: {}ms",
                            response.statusCode(), elapsedMs);
                    return parseResponse(response.body());
                });
    }

    private static BuildStructure parseResponse(String responseBody) {
        try {
            JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
            String content = response.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            CraftAssistMod.LOGGER.debug("[CraftAssist] LLM 回應內容:\n{}", content);

            return GSON.fromJson(content, BuildStructure.class);
        } catch (Exception e) {
            CraftAssistMod.LOGGER.error("[CraftAssist] 無法解析 API 回應", e);
            return null;
        }
    }
}
