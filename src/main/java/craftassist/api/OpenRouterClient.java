package craftassist.api;

import craftassist.CraftAssistMod;
import craftassist.builder.BuildStructure;
import craftassist.config.ModConfig;
import com.google.gson.*;
import net.minecraft.core.Direction;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public class OpenRouterClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final Gson GSON = new Gson();

    /**
     * 第一階段：創意規劃（回傳純文字藍圖）
     */
    public static CompletableFuture<String> generatePlan(String description, Direction facing, ModConfig config) {
        String systemPrompt = PromptBuilder.buildPlanningPrompt();
        String userPrompt = "Build: " + description + "\nPlayer is facing: " + facing.getName()
                + " (the building entrance should face the player)";

        CraftAssistMod.LOGGER.debug("[CraftAssist] 第一階段 API 請求 | 模型: {} | 描述: {}", config.getModel(), description);

        return sendRequestWithRetry(systemPrompt, userPrompt, config, false, "第一階段", 0)
                .thenApply(body -> parseTextResponse(body, "第一階段"));
    }

    /**
     * 第二階段：完整建築生成（回傳建築 JSON）
     */
    public static CompletableFuture<BuildStructure> generateBuilding(String blueprint, ModConfig config) {
        String systemPrompt = PromptBuilder.buildBuildingPrompt(blueprint, config.getMaxBlocks());
        String userPrompt = "Generate the complete building JSON following the blueprint above.";

        CraftAssistMod.LOGGER.debug("[CraftAssist] 第二階段 API 請求 | 模型: {}", config.getModel());

        return sendRequestWithRetry(systemPrompt, userPrompt, config, true, "第二階段", 0)
                .thenApply(body -> parseBuildResponse(body, "第二階段"));
    }

    private static CompletableFuture<String> sendRequestWithRetry(
            String systemPrompt, String userPrompt, ModConfig config,
            boolean jsonFormat, String stage, int attempt) {

        return sendRequest(systemPrompt, userPrompt, config, jsonFormat, stage)
                .exceptionallyCompose(ex -> {
                    Throwable cause = unwrapCause(ex);

                    // 將原始異常轉為 ApiException
                    ApiException apiEx;
                    if (cause instanceof ApiException ae) {
                        apiEx = ae;
                    } else if (cause instanceof HttpTimeoutException) {
                        apiEx = new ApiException(ApiException.Type.TIMEOUT, cause);
                    } else if (cause instanceof ConnectException) {
                        apiEx = new ApiException(ApiException.Type.NETWORK_ERROR, cause);
                    } else {
                        apiEx = new ApiException(ApiException.Type.NETWORK_ERROR, cause.getMessage());
                    }

                    int maxRetries = config.getMaxRetries();
                    if (attempt < maxRetries && apiEx.isRetriable()) {
                        long delayMs = (long) Math.pow(2, attempt) * 1000;
                        CraftAssistMod.LOGGER.warn("[CraftAssist] {} 失敗，{}ms 後重試 ({}/{}): {}",
                                stage, delayMs, attempt + 1, maxRetries, apiEx.getMessage());

                        return CompletableFuture.supplyAsync(() -> null,
                                        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS))
                                .thenCompose(v -> sendRequestWithRetry(
                                        systemPrompt, userPrompt, config, jsonFormat, stage, attempt + 1));
                    }

                    return CompletableFuture.failedFuture(apiEx);
                });
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
                        CraftAssistMod.LOGGER.error("[CraftAssist] {} API 錯誤 | HTTP {} | 耗時: {}ms",
                                stage, response.statusCode(), elapsedMs);

                        throw switch (response.statusCode()) {
                            case 401, 403 -> new ApiException(ApiException.Type.AUTHENTICATION);
                            case 429 -> new ApiException(ApiException.Type.RATE_LIMIT);
                            default -> {
                                if (response.statusCode() >= 500) {
                                    yield new ApiException(ApiException.Type.SERVER_ERROR,
                                            "HTTP " + response.statusCode());
                                }
                                yield new ApiException(ApiException.Type.NETWORK_ERROR,
                                        "HTTP " + response.statusCode());
                            }
                        };
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
            throw new ApiException(ApiException.Type.PARSE_ERROR, e);
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
            CraftAssistMod.LOGGER.error("[CraftAssist] {} 無法解析建築 JSON", stage, e);
            throw new ApiException(ApiException.Type.PARSE_ERROR, e);
        }
    }

    private static Throwable unwrapCause(Throwable ex) {
        if (ex instanceof CompletionException && ex.getCause() != null) {
            return ex.getCause();
        }
        return ex;
    }
}
