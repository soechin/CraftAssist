# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 建置指令

```bash
./gradlew build          # 編譯並產出 build/libs/craftassist-*.jar
./gradlew runServer      # 啟動開發測試伺服器
./gradlew runClient      # 啟動開發測試客戶端
```

## 技術環境

- Minecraft 1.21.11 / Fabric Loader 0.18.1 / Fabric API 0.141.3
- Fabric Loom 1.14.10 / Gradle 9.2 / Java 21
- 使用 **Mojang Official Mappings**（非 Yarn）

### 1.21.11 Mapping 注意事項

- `ResourceLocation` 已重新命名為 `net.minecraft.resources.Identifier`
- 權限系統改為 `PermissionSet`：使用 `source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)` 而非舊版 `source.hasPermission(2)`
- 方塊註冊表使用 `BuiltInRegistries.BLOCK`，方塊 ID 驗證透過 `Identifier.tryParse()`

## 架構

玩家透過 `/ca build <自然語言描述>` 指令觸發 → OpenRouter API 呼叫 LLM 生成建築 JSON → 分批放置方塊（每 tick 放置 `blocksPerTick` 個）。`/ca undo` 可復原上一次建築操作。

### 指令

- `/ca build <描述>` — AI 生成並分批放置建築
- `/ca undo` — 復原上一次建築操作（per-player）

### 執行緒模型（關鍵）

`BuildCommand`（伺服器執行緒）→ `OpenRouterClient.sendAsync()`（背景 HTTP 執行緒）→ `server.execute()`（回到伺服器執行緒）→ `BatchPlacementManager`（每 tick `END_SERVER_TICK` 分批放置）。HTTP 請求必須非同步，方塊操作必須在伺服器執行緒。

### 模組

- **command/** — Brigadier 指令註冊（`/ca` 前綴），透過 `CommandRegistrationCallback.EVENT`。`CraftAssistCommand` 負責指令樹，`BuildCommand` 與 `UndoCommand` 為子指令處理器
- **api/** — OpenRouter HTTP 客戶端（`java.net.HttpClient`）與 LLM 系統提示詞。API 回應強制 `response_format: json_object`
- **builder/** — 建築結構資料模型（區域壓縮 JSON）、方塊 ID 驗證（從 `BuiltInRegistries` 動態查詢）、放置引擎（`BlockPlacementEngine.preparePlacements()` 預計算方塊清單）、`BatchPlacementManager` 分批放置與進度顯示
- **undo/** — `UndoData`（BlockPos + BlockState 快照）、`UndoManager`（per-player ConcurrentHashMap）
- **config/** — Gson 讀寫 `config/craftassist.json`（含 API key、模型、blocksPerTick）

### LLM 建築 JSON 格式

```json
{
  "regions": [
    {"block": "minecraft:stone", "from": [0,0,0], "to": [4,0,4], "hollow": false, "facing": null}
  ]
}
```

座標為玩家位置的相對座標，`hollow: true` 只放外殼，`facing` 用於方向性方塊。

### 批次放置系統

`BlockPlacementEngine.preparePlacements()` 預計算所有 `BlockPlacement(pos, state)` 清單 → `BatchPlacementManager` 透過 `ServerTickEvents.END_SERVER_TICK` 每 tick 放置 `blocksPerTick` 個方塊，並在 Action Bar 顯示進度條。放置前記錄原始 BlockState 供 undo。Undo 也走批次還原。

建築/復原進行中時，拒絕新的 `/ca build` 或 `/ca undo` 指令。
