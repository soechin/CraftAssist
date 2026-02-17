# CraftAssist

AI 驅動的 Minecraft 建築助手。用自然語言描述你想要的建築，AI 就會幫你蓋出來。

## 功能特色

- **自然語言建築** — 輸入 `/ca build 一座中世紀城堡`，AI 自動生成並放置建築
- **批次放置** — 方塊分批放置，不會造成伺服器卡頓，並在 Action Bar 顯示進度
- **一鍵復原** — `/ca undo` 還原上一次建築操作
- **入口偵測** — 自動旋轉建築使門面對玩家
- **安全防護** — 方塊白名單驗證、API 速率限制、座標範圍限制

## 需求

- Minecraft 1.21.11
- Fabric Loader >= 0.18.1
- Fabric API
- Java 21
- [OpenRouter](https://openrouter.ai/) API Key

## 安裝

1. 確認已安裝 [Fabric Loader](https://fabricmc.net/) 和 Fabric API
2. 從原始碼編譯或下載 `craftassist-1.0.0.jar`
3. 將 JAR 放入 `.minecraft/mods/` 資料夾
4. 啟動遊戲

## 設定

首次啟動後，編輯 `config/craftassist.json`：

```json
{
  "apiKey": "你的 OpenRouter API Key",
  "model": "anthropic/claude-sonnet-4-5",
  "blocksPerTick": 500,
  "maxBlocks": 1000000,
  "timeoutSeconds": 60,
  "maxRetries": 2
}
```

| 欄位 | 說明 | 預設值 |
|------|------|--------|
| `apiKey` | OpenRouter API Key（必填） | `""` |
| `model` | LLM 模型名稱 | `anthropic/claude-sonnet-4-5` |
| `blocksPerTick` | 每 tick 放置的方塊數 | `500` |
| `maxBlocks` | 單次建築最大方塊數 | `1000000` |
| `timeoutSeconds` | API 請求逾時秒數 | `60` |
| `maxRegionVolume` | 單一區域最大體積 | `100000` |
| `maxCoordinate` | 座標最大距離（相對玩家） | `200` |
| `rateLimitTokens` | 速率限制 token 數 | `3` |
| `rateLimitRefillSeconds` | 速率限制補充間隔（秒） | `60` |
| `maxRetries` | API 失敗重試次數 | `2` |

設定修改後可在遊戲中執行 `/ca reload` 重新載入。

## 使用方式

```
/ca build <自然語言描述>
/ca undo
/ca reload
```

**範例：**

```
/ca build 一座三層樓的石磚塔樓，頂部有旗幟
/ca build a small wooden cabin with a garden
/ca undo
```

建築或復原進行中時，無法執行新的 build 或 undo 指令。

## 從原始碼建置

```bash
git clone <repo-url>
cd CraftAssist
./gradlew build
```

編譯產出位於 `build/libs/craftassist-*.jar`。

開發用指令：

```bash
./gradlew runServer    # 啟動開發測試伺服器
./gradlew runClient    # 啟動開發測試客戶端
```

## 授權

[MIT License](LICENSE)
