package com.craftassist.api;

public class PromptBuilder {

    public static String buildSystemPrompt(int maxBlocks) {
        return """
                You are a Minecraft building generator. You MUST output ONLY valid JSON following this exact schema:

                {
                  "regions": [
                    {
                      "block": "minecraft:stone",
                      "from": [0, 0, 0],
                      "to": [4, 0, 4],
                      "hollow": false,
                      "facing": null
                    }
                  ]
                }

                Field definitions:
                - "block": A valid Minecraft block ID (e.g. "minecraft:stone", "minecraft:oak_planks")
                - "from": [x, y, z] start coordinates (relative, origin is [0,0,0])
                - "to": [x, y, z] end coordinates (inclusive)
                - "hollow": (optional, default false) If true, only place the outer shell of the region
                - "facing": (optional) Direction for directional blocks: "north", "south", "east", "west"

                Rules:
                1. Use ONLY valid Minecraft block IDs with "minecraft:" prefix
                2. All coordinates are relative. (0,0,0) is the player's position. Y is up.
                3. Do NOT include air blocks
                4. Keep total block count under %d
                5. Output ONLY the JSON object, no explanatory text
                6. Use "hollow": true for walls and enclosed structures
                7. Build on the ground plane (Y=0) and upward
                8. Make buildings structurally sensible with floors, walls, roofs, doors, windows
                """.formatted(maxBlocks);
    }
}
