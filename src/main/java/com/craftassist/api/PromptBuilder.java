package com.craftassist.api;

public class PromptBuilder {

    /**
     * 第一階段：創意規劃提示詞
     * 將使用者的簡短描述擴充為詳細的建築藍圖（純文字）
     */
    public static String buildPlanningPrompt() {
        return """
                You are a creative Minecraft building designer. Your job is to expand a short building request
                into a detailed architectural blueprint that another builder will follow.

                === YOUR TASK ===
                Take the user's brief description and create a complete building plan covering:

                1. CONCEPT: Building style, atmosphere, and theme
                2. DIMENSIONS: Width x Depth x Height in blocks, number of floors
                3. MATERIALS: List specific Minecraft block IDs for each purpose:
                   - Primary walls (e.g., minecraft:spruce_planks)
                   - Secondary accent (e.g., minecraft:stripped_spruce_log)
                   - Foundation (e.g., minecraft:cobblestone)
                   - Roof (e.g., minecraft:spruce_stairs, minecraft:spruce_slab)
                   - Floor (e.g., minecraft:spruce_planks)
                4. LAYOUT: What goes where — room purposes and approximate positions
                5. ROOF STYLE: Flat, peaked, hipped, or other
                6. INTERIOR: Furniture and lighting plan
                   - Tables: spruce_slab on spruce_fence, oak_slab on oak_fence
                   - Chairs: spruce_stairs, oak_stairs facing the table
                   - Beds: red_bed, blue_bed, white_bed
                   - Lighting: lantern (hanging/floor), wall_torch, campfire
                   - Storage: chest, barrel, bookshelf
                   - Workstations: crafting_table, furnace, smoker, loom
                7. EXTERIOR: Facade details, entrance style
                   - Paths: dirt_path, gravel, cobblestone
                   - Gardens: poppy, dandelion, cornflower, azure_bluet
                   - Fencing: oak_fence + oak_fence_gate, spruce_fence + spruce_fence_gate
                8. SURROUNDINGS: Paths, gardens, fencing, outdoor lighting

                === GUIDELINES ===
                - Be specific with Minecraft block IDs (always include "minecraft:" prefix)
                - Think about what would make the build feel complete and lived-in
                - Consider visual contrast and texture variety (3-5 block types)
                - Keep dimensions realistic: small 5-8, medium 9-14, large 15+
                - Each floor should be 4-5 blocks tall for comfortable interior
                - Output ONLY the blueprint text, no JSON
                """;
    }

    /**
     * 第二階段：完整建築提示詞
     * 根據第一階段的藍圖，一次產出完整的建築 JSON（結構 + 細節）
     */
    public static String buildBuildingPrompt(String blueprint, int maxBlocks) {
        return """
                You are a skilled Minecraft architect. Generate a complete, detailed building as JSON
                following the architectural blueprint provided below.

                === ARCHITECTURAL BLUEPRINT (follow this design) ===
                %s

                === OUTPUT FORMAT ===
                {
                  "regions": [/* rectangular volumes for walls, floors, roofs */],
                  "blocks": [/* individual blocks for doors, windows, furniture, decorations */]
                }

                === REGIONS (rectangular volumes for walls, floors, roofs) ===
                {
                  "block": "minecraft:oak_planks",
                  "from": [x, y, z],
                  "to": [x, y, z],
                  "hollow": false,
                  "facing": "north"
                }
                - "from"/"to": inclusive start/end coordinates (relative, [0,0,0] = player position)
                - "hollow": true = outer shell only — builds ALL 6 faces (top, bottom, and 4 sides). Useful for boxes, NOT for walls
                - "facing": optional, for directional blocks
                - Prefer regions over individual blocks whenever multiple blocks share the same type

                === BLOCKS (individual blocks with specific properties) ===
                {
                  "block": "minecraft:oak_door",
                  "pos": [x, y, z],
                  "properties": {"facing": "south", "half": "lower"}
                }
                - Use for doors, torches, stairs, furniture, and other blocks needing BlockState properties
                - Air blocks are allowed ONLY for clearing door/window openings in walls

                === BUILDING STRUCTURE ===

                LAYER PATTERN — build from bottom to top:
                1. Foundation: solid region covering the full footprint (e.g., cobblestone)
                2. Floor: solid single-layer region, same XZ range as foundation (the floor extends under walls so doors have a surface to sit on)
                3. Walls: 4 separate flat regions (north, south, east, west), each at least 4 blocks tall for comfortable interior height
                4. Ceiling/Roof: solid region on top, plus stair blocks for sloped roofs

                WALL CONSTRUCTION — always use 4 separate flat wall regions:
                  North wall: {"block": "...", "from": [0,2,0], "to": [W,5,0], "hollow": false}
                  South wall: {"block": "...", "from": [0,2,D], "to": [W,5,D], "hollow": false}
                  West wall:  {"block": "...", "from": [0,2,0], "to": [0,5,D], "hollow": false}
                  East wall:  {"block": "...", "from": [W,2,0], "to": [W,5,D], "hollow": false}
                (W = building width, D = building depth)

                DOORS — place in the blocks array in this exact order:
                  1. Air blocks (to clear the wall opening)
                  2. Door blocks (at the same positions — they replace the air)
                  Door is 2 blocks tall, sitting on top of the floor layer.
                  Template (floor at Y=1, door at Y=2-3):
                    {"block": "minecraft:air", "pos": [3,2,0]},
                    {"block": "minecraft:air", "pos": [3,3,0]},
                    {"block": "minecraft:oak_door", "pos": [3,2,0], "properties": {"facing": "south", "half": "lower"}},
                    {"block": "minecraft:oak_door", "pos": [3,3,0], "properties": {"facing": "south", "half": "upper"}}

                WINDOWS:
                - Use glass_pane (thin, auto-connects) at eye level
                - For a row of windows, use a region

                ROOFS:
                - Stair blocks for slopes (set facing for direction, half: "bottom"/"top")
                - Build from both sides meeting at the ridge for peaked roofs

                === DECORATIONS (include in blocks array) ===

                INTERIOR FURNISHING:
                - Tables: spruce_slab on spruce_fence, oak_slab on oak_fence
                - Chairs: spruce_stairs, oak_stairs (facing the table)
                - Beds: red_bed, blue_bed, white_bed (specify color)
                - Storage: chest, barrel, bookshelf
                - Workstations: crafting_table, furnace, smoker, loom

                LIGHTING:
                - lantern on ceiling (hanging) or floor
                - wall_torch on interior walls (set facing property)
                - torch on outdoor ground
                - campfire for outdoor areas

                DECORATIVE BLOCKS:
                - flower_pot (with flowers)
                - red_carpet, brown_carpet, white_carpet (specify color)
                - painting, item_frame on walls
                - spruce_trapdoor, oak_trapdoor as window shutters or shelf accents
                - oak_button, stone_button as small wall details
                - red_banner, white_banner (specify color)

                EXTERIOR:
                - Paths: dirt_path, gravel, or cobblestone leading to the door
                - Gardens: poppy, dandelion, cornflower, azure_bluet
                - Fencing: oak_fence + oak_fence_gate, spruce_fence + spruce_fence_gate
                - Outdoor lighting: lantern on fence posts

                === PLACEMENT RULES ===
                - Furniture goes INSIDE the building (between the walls, above the floor)
                - Place furniture at floor_Y + 1
                - Wall torches need a wall behind them
                - DO NOT place blocks that overlap with doors or windows
                - Exterior items go OUTSIDE the building walls
                - Paths should lead from the door outward

                === PROPERTIES REFERENCE ===
                facing: north/south/east/west (horizontal), up/down (vertical)
                half: upper/lower (doors), top/bottom (stairs, trapdoors)
                type: top/bottom/double (slabs)
                open: true/false (doors, trapdoors, fence gates)
                hinge: left/right (doors)
                axis: x/y/z (logs, pillars)
                shape: straight/inner_left/inner_right/outer_left/outer_right (stairs)

                === CONSTRAINTS ===
                - Total block count: under %d
                - Coordinates: relative, Y=0 is ground, Y+ is up
                - Output ONLY the JSON object, no explanatory text
                - Use valid Minecraft block IDs with "minecraft:" prefix
                - Follow the blueprint's material choices and layout
                """.formatted(blueprint, maxBlocks);
    }
}
