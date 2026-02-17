package craftassist.api;

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
                2. DIMENSIONS: Width (X) x Depth (Z) x Wall Height in blocks
                   - IMPORTANT: Unless the user explicitly asks for multiple floors, design a SINGLE-FLOOR building.
                     A well-crafted single-floor building is far better than a buggy multi-floor one.
                   - Small: 5-8, Medium: 9-14, Large: 15+
                3. MATERIALS: Specific Minecraft block IDs for each purpose (walls, floor, roof, accents)
                4. LAYOUT: Room purposes and positions (for single-floor: one open room or 2-3 simple areas)
                5. ROOF STYLE: flat, gable (peaked), or hipped
                6. INTERIOR: Furniture and lighting
                7. EXTERIOR: Entrance style, paths, gardens
                8. ENTRANCE: Which wall the entrance is on (the builder will rotate it to face the player)

                === GUIDELINES ===
                - Use ONLY block IDs from the AVAILABLE BLOCKS list below
                - Think about visual contrast and texture variety (3-5 block types)
                - Wall height should be 4-5 blocks for comfortable interior
                - Do NOT design internal staircases or multi-floor layouts unless explicitly requested
                - Do NOT use item_frame, painting, or armor_stand — these are entities, not blocks
                - Output ONLY the blueprint text, no JSON

                """ + BLOCK_WHITELIST;
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

                === REGIONS (rectangular volumes) ===
                {
                  "block": "minecraft:oak_planks",
                  "from": [x, y, z],
                  "to": [x, y, z],
                  "hollow": false,
                  "properties": {"facing": "east", "half": "bottom"}
                }
                - "from"/"to": inclusive start/end coordinates (relative, [0,0,0] = building origin)
                - "hollow": true = outer shell only (ALL 6 faces). Use ONLY for complete box structures.
                  NEVER use hollow for interior partition walls — use flat regions instead.
                - "properties": optional BlockState properties (facing, half, type, axis, shape, etc.)
                - "exclude": optional list of rectangular cutouts: [[[x1,y1,z1],[x2,y2,z2]]]
                  Use this to leave openings in walls for doors/windows instead of placing air blocks.
                  IMPORTANT: exclude coordinates use the SAME global relative coordinate system as "from"/"to".
                  For a door on a wall at X=6: exclude [[[6,2,3],[6,3,3]]], NOT [[[0,2,3],[0,3,3]]].
                  Each exclude MUST be exactly the minimum opening needed (1 block wide × 2 blocks tall for doors).
                  Do NOT exclude extra blocks around the door opening.

                === BLOCKS (individual blocks with properties) ===
                {
                  "block": "minecraft:oak_door",
                  "pos": [x, y, z],
                  "properties": {"facing": "south", "half": "lower"}
                }

                === COORDINATE SYSTEM & LAYER REFERENCE ===
                X+ = east, X- = west, Y+ = up, Y- = down, Z+ = south, Z- = north
                [0,0,0] = ground level at building origin

                STANDARD LAYER HEIGHTS (single-floor building):
                  Y=0  Foundation (cobblestone or stone_bricks)
                  Y=1  Floor surface (planks) — the walking surface
                  Y=2  Wall starts, door bottom, furniture level
                  Y=3  Window level, wall_torch level
                  Y=4  Upper wall (gives 3-block interior: Y=2,3,4 clear)
                  Y=5  Wall top
                  Y=6+ Roof (stairs/slabs stepping up)

                WALL NAMING:
                  North wall = the wall at min-Z
                  South wall = the wall at max-Z (Z=D where D=depth-1)
                  West wall  = the wall at min-X (X=0)
                  East wall  = the wall at max-X (X=W where W=width-1)

                === CRITICAL SPATIAL RULES ===

                1. WALL CONSTRUCTION — use 4 flat regions that share corners:
                   North wall: from [0,2,0] to [W,5,0]
                   South wall: from [0,2,D] to [W,5,D]
                   West wall:  from [0,2,0] to [0,5,D]
                   East wall:  from [W,2,0] to [W,5,D]
                   (W = width-1, D = depth-1)
                   Walls MUST share corner coordinates to be sealed!

                2. FURNITURE MUST BE INSIDE — all interior items at X in [1..W-1], Z in [1..D-1]:
                   - Interior X range: 1 to W-1 (NOT 0 or W, those are walls)
                   - Interior Z range: 1 to D-1 (NOT 0 or D, those are walls)
                   - Furniture Y = 2 (on top of the floor at Y=1)

                3. DOOR OPENINGS — use "exclude" on the wall region to leave space:
                   Then place the door block at the excluded position.
                   Door bottom at Y=2, top at Y=3. Door "facing" = direction player faces when walking through.

                4. ENTRANCE CLEARANCE — BOTH sides of the door MUST be clear:
                   OUTSIDE: No fences, flowers, lanterns, or any block at Y≥2 within 3 blocks of the door exterior.
                   INSIDE: The 2 blocks directly behind the door (inside the building) MUST be walkable.
                     No furniture, fireplaces, bookshelves, or any solid block at Y≥2 within 2 blocks of the door interior.
                   Paths and ground-level decorations (Y=0 or Y=1) are OK.

                5. WINDOWS — place glass_pane blocks in the "blocks" array at wall positions.
                   Recommended at Y=3 (eye level).

                6. TORCH vs WALL_TORCH:
                   - "minecraft:torch" is ground-only (placed on top of a block). It has NO facing property.
                   - "minecraft:wall_torch" is wall-mounted. It REQUIRES "facing" property (direction it faces AWAY from the wall).
                     A wall_torch on the north wall (Z=0) should have facing=south (facing into the room).
                   NEVER give "torch" a "facing" property — it will be ignored.

                7. FLOWER POTS — use the combined block ID, NOT separate flower_pot + flower:
                   Use: potted_poppy, potted_dandelion, potted_red_tulip, potted_blue_orchid, etc.
                   Do NOT place flower_pot at [x,y,z] and then poppy at the same [x,y,z].

                8. GABLE ROOF with stairs — each row of stairs MUST step up one Y-level:
                   The roof is NOT flat! Each stair row is ONE BLOCK HIGHER than the previous one.
                   For a 7-wide building (W=6), wall top at Y=5:
                     X=0 at Y=6 facing east, X=6 at Y=6 facing west  (first row, both sides)
                     X=1 at Y=7 facing east, X=5 at Y=7 facing west  (second row, higher)
                     X=2 at Y=8 facing east, X=4 at Y=8 facing west  (third row, higher)
                     X=3 at Y=9 slab (ridge, highest point)
                   WRONG: all stairs at Y=6 (this creates a flat roof, not a gable!)
                   For flat roofs: use slab regions instead of stairs.

                9. GABLE ROOF REQUIRES GABLE WALLS:
                   A gable roof has triangular gaps at both ends (Z=0 and Z=D).
                   You MUST fill these triangles with wall blocks (matching the wall material).
                   Example for a 7-wide building (wall top Y=5, roof starts Y=6):
                     North gable: from [1,6,0] to [5,6,0], from [2,7,0] to [4,7,0], from [3,8,0] to [3,8,0]
                     South gable: same pattern at Z=D
                   Without gable walls, the interior is exposed to the sky!

                === COMPLETE EXAMPLE (7-wide, 9-deep cottage) ===
                %s

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
                - Use ONLY block IDs from the AVAILABLE BLOCKS list below
                - Output ONLY the JSON object, no explanatory text
                - Follow the blueprint's material choices and layout
                - Follow the blueprint's entrance wall
                - Do NOT use item_frame, painting, or armor_stand — these are entities, not blocks

                """.formatted(blueprint, EXAMPLE_HOUSE, maxBlocks) + BLOCK_WHITELIST;
    }

    /**
     * 第三階段：修正提示詞
     * 讓 LLM 根據驗證結果修正建築 JSON
     */
    public static String buildFixPrompt(String originalJson, String issuesReport) {
        return """
                You generated a Minecraft building JSON, but validation found these issues:

                %s

                Here is your original JSON:
                %s

                Please fix ONLY the reported issues and output the corrected complete JSON.
                Do not change the overall design, materials, or layout — only fix the specific problems.
                Output ONLY the JSON object, no explanatory text.
                """.formatted(issuesReport, originalJson);
    }

    // ==================== 範例建築 ====================

    private static final String EXAMPLE_HOUSE = """
            {
              "regions": [
                {"block": "minecraft:cobblestone", "from": [0,0,0], "to": [6,0,8], "hollow": false},
                {"block": "minecraft:spruce_planks", "from": [0,1,0], "to": [6,1,8], "hollow": false},
                {"block": "minecraft:spruce_planks", "from": [0,2,0], "to": [6,5,0], "hollow": false},
                {"block": "minecraft:spruce_planks", "from": [0,2,8], "to": [6,5,8], "hollow": false,
                 "exclude": [[[3,2,8],[3,3,8]]]},
                {"block": "minecraft:spruce_planks", "from": [0,2,0], "to": [0,5,8], "hollow": false},
                {"block": "minecraft:spruce_planks", "from": [6,2,0], "to": [6,5,8], "hollow": false},
                {"block": "minecraft:spruce_planks", "from": [1,6,0], "to": [5,6,0], "hollow": false},
                {"block": "minecraft:spruce_planks", "from": [2,7,0], "to": [4,7,0], "hollow": false},
                {"block": "minecraft:spruce_planks", "from": [1,6,8], "to": [5,6,8], "hollow": false},
                {"block": "minecraft:spruce_planks", "from": [2,7,8], "to": [4,7,8], "hollow": false},
                {"block": "minecraft:spruce_stairs", "from": [0,6,0], "to": [0,6,8],
                 "properties": {"facing": "east", "half": "bottom"}},
                {"block": "minecraft:spruce_stairs", "from": [6,6,0], "to": [6,6,8],
                 "properties": {"facing": "west", "half": "bottom"}},
                {"block": "minecraft:spruce_stairs", "from": [1,7,0], "to": [1,7,8],
                 "properties": {"facing": "east", "half": "bottom"}},
                {"block": "minecraft:spruce_stairs", "from": [5,7,0], "to": [5,7,8],
                 "properties": {"facing": "west", "half": "bottom"}},
                {"block": "minecraft:spruce_slab", "from": [2,8,0], "to": [4,8,8], "hollow": false}
              ],
              "blocks": [
                {"block": "minecraft:spruce_door", "pos": [3,2,8],
                 "properties": {"facing": "north", "half": "lower", "hinge": "right"}},
                {"block": "minecraft:spruce_door", "pos": [3,3,8],
                 "properties": {"facing": "north", "half": "upper", "hinge": "right"}},
                {"block": "minecraft:glass_pane", "pos": [2,3,0]},
                {"block": "minecraft:glass_pane", "pos": [4,3,0]},
                {"block": "minecraft:glass_pane", "pos": [0,3,4]},
                {"block": "minecraft:glass_pane", "pos": [6,3,4]},
                {"block": "minecraft:red_bed", "pos": [1,2,1],
                 "properties": {"facing": "south", "part": "head"}},
                {"block": "minecraft:red_bed", "pos": [1,2,2],
                 "properties": {"facing": "south", "part": "foot"}},
                {"block": "minecraft:crafting_table", "pos": [5,2,1]},
                {"block": "minecraft:chest", "pos": [5,2,2],
                 "properties": {"facing": "west"}},
                {"block": "minecraft:furnace", "pos": [5,2,3],
                 "properties": {"facing": "west"}},
                {"block": "minecraft:spruce_fence", "pos": [3,2,4]},
                {"block": "minecraft:spruce_slab", "pos": [3,3,4],
                 "properties": {"type": "bottom"}},
                {"block": "minecraft:lantern", "pos": [3,7,4],
                 "properties": {"hanging": "true"}},
                {"block": "minecraft:wall_torch", "pos": [1,3,0],
                 "properties": {"facing": "south"}},
                {"block": "minecraft:potted_poppy", "pos": [5,2,7]},
                {"block": "minecraft:dirt_path", "pos": [3,0,9]},
                {"block": "minecraft:dirt_path", "pos": [3,0,10]},
                {"block": "minecraft:dirt_path", "pos": [3,0,11]},
                {"block": "minecraft:poppy", "pos": [2,1,9]},
                {"block": "minecraft:dandelion", "pos": [4,1,9]}
              ]
            }
            Key points in this example:
            - Walls share corners (all 4 walls go from Y=2 to Y=5, covering [0..6] x [0..8])
            - GABLE ROOF steps up: X=0/6 at Y=6, X=1/5 at Y=7, ridge X=2~4 at Y=8 (each row 1 Y higher!)
            - GABLE WALLS: triangular fill at Z=0 and Z=8 ([1,6,0]→[5,6,0], [2,7,0]→[4,7,0])
              These fill the gaps between the wall tops and the sloped roof
            - Hanging lantern at Y=7 directly under the ridge slab at Y=8
            - Door uses "exclude" on south wall (Z=8), exclude is exactly 1 wide × 2 tall (minimum opening)
            - Door interior clear: no furniture at Z=7 near X=3 (2-block clearance behind door)
            - Furniture (bed, chest, crafting_table) all at X=1..5, Z=1..7 (inside walls)
            - Door exterior (Z=9,10,11) only has ground-level path and flowers, nothing blocking at Y≥2
            - wall_torch on north wall (Z=0) has facing=south (into the room), NOT facing=north
            - Flower pot uses combined ID: potted_poppy (NOT separate flower_pot + poppy)
            - Roof stairs use "properties" with correct facing (east on left, west on right)
            """;

    // ==================== 方塊白名單 ====================

    private static final String BLOCK_WHITELIST = """

            === AVAILABLE BLOCKS (use ONLY these IDs) ===

            STONE & BRICK:
            minecraft:stone, minecraft:cobblestone, minecraft:stone_bricks, minecraft:mossy_cobblestone,
            minecraft:mossy_stone_bricks, minecraft:andesite, minecraft:polished_andesite, minecraft:granite,
            minecraft:polished_granite, minecraft:diorite, minecraft:polished_diorite, minecraft:bricks,
            minecraft:chiseled_stone_bricks, minecraft:smooth_stone, minecraft:sandstone, minecraft:red_sandstone,
            minecraft:quartz_block, minecraft:smooth_quartz, minecraft:chiseled_quartz_block,
            minecraft:prismarine, minecraft:dark_prismarine, minecraft:deepslate_bricks, minecraft:deepslate_tiles,
            minecraft:tuff_bricks, minecraft:polished_tuff

            WOOD PLANKS:
            minecraft:oak_planks, minecraft:spruce_planks, minecraft:birch_planks, minecraft:dark_oak_planks,
            minecraft:jungle_planks, minecraft:acacia_planks, minecraft:mangrove_planks, minecraft:cherry_planks,
            minecraft:crimson_planks, minecraft:warped_planks, minecraft:bamboo_planks

            LOGS & WOOD:
            minecraft:oak_log, minecraft:spruce_log, minecraft:birch_log, minecraft:dark_oak_log,
            minecraft:jungle_log, minecraft:acacia_log, minecraft:mangrove_log, minecraft:cherry_log,
            minecraft:stripped_oak_log, minecraft:stripped_spruce_log, minecraft:stripped_birch_log,
            minecraft:stripped_dark_oak_log, minecraft:stripped_jungle_log, minecraft:stripped_acacia_log,
            minecraft:oak_wood, minecraft:spruce_wood, minecraft:stripped_oak_wood, minecraft:stripped_spruce_wood

            STAIRS (use "properties" for facing and half):
            minecraft:oak_stairs, minecraft:spruce_stairs, minecraft:birch_stairs, minecraft:dark_oak_stairs,
            minecraft:jungle_stairs, minecraft:acacia_stairs, minecraft:cherry_stairs,
            minecraft:cobblestone_stairs, minecraft:stone_brick_stairs, minecraft:sandstone_stairs,
            minecraft:brick_stairs, minecraft:quartz_stairs, minecraft:andesite_stairs, minecraft:granite_stairs,
            minecraft:polished_andesite_stairs, minecraft:purpur_stairs

            SLABS (use "properties" for type: top/bottom/double):
            minecraft:oak_slab, minecraft:spruce_slab, minecraft:birch_slab, minecraft:dark_oak_slab,
            minecraft:jungle_slab, minecraft:acacia_slab, minecraft:cherry_slab,
            minecraft:cobblestone_slab, minecraft:stone_brick_slab, minecraft:smooth_stone_slab,
            minecraft:sandstone_slab, minecraft:brick_slab, minecraft:quartz_slab, minecraft:polished_andesite_slab

            GLASS:
            minecraft:glass, minecraft:glass_pane,
            minecraft:white_stained_glass, minecraft:white_stained_glass_pane,
            minecraft:light_gray_stained_glass_pane, minecraft:brown_stained_glass_pane

            DOORS (use "properties" for facing, half, hinge):
            minecraft:oak_door, minecraft:spruce_door, minecraft:birch_door, minecraft:dark_oak_door,
            minecraft:jungle_door, minecraft:acacia_door, minecraft:cherry_door, minecraft:iron_door,
            minecraft:crimson_door, minecraft:warped_door

            TRAPDOORS:
            minecraft:oak_trapdoor, minecraft:spruce_trapdoor, minecraft:birch_trapdoor,
            minecraft:dark_oak_trapdoor, minecraft:iron_trapdoor

            FENCES & GATES:
            minecraft:oak_fence, minecraft:spruce_fence, minecraft:birch_fence, minecraft:dark_oak_fence,
            minecraft:oak_fence_gate, minecraft:spruce_fence_gate, minecraft:birch_fence_gate,
            minecraft:dark_oak_fence_gate

            WALLS:
            minecraft:cobblestone_wall, minecraft:stone_brick_wall, minecraft:brick_wall,
            minecraft:andesite_wall, minecraft:granite_wall, minecraft:sandstone_wall

            FURNITURE & FUNCTIONAL:
            minecraft:chest, minecraft:barrel, minecraft:bookshelf, minecraft:crafting_table,
            minecraft:furnace, minecraft:smoker, minecraft:blast_furnace, minecraft:loom,
            minecraft:lectern, minecraft:cauldron, minecraft:composter, minecraft:anvil,
            minecraft:enchanting_table, minecraft:brewing_stand, minecraft:jukebox, minecraft:note_block

            BEDS:
            minecraft:red_bed, minecraft:blue_bed, minecraft:white_bed, minecraft:green_bed,
            minecraft:black_bed, minecraft:brown_bed, minecraft:cyan_bed, minecraft:gray_bed,
            minecraft:light_blue_bed, minecraft:light_gray_bed, minecraft:lime_bed, minecraft:magenta_bed,
            minecraft:orange_bed, minecraft:pink_bed, minecraft:purple_bed, minecraft:yellow_bed

            LIGHTING:
            minecraft:lantern, minecraft:soul_lantern, minecraft:torch, minecraft:wall_torch,
            minecraft:soul_torch, minecraft:soul_wall_torch, minecraft:campfire, minecraft:soul_campfire,
            minecraft:sea_lantern, minecraft:glowstone, minecraft:shroomlight

            PLANTS & FLOWERS:
            minecraft:short_grass, minecraft:tall_grass, minecraft:fern, minecraft:poppy,
            minecraft:dandelion, minecraft:cornflower, minecraft:azure_bluet, minecraft:allium,
            minecraft:blue_orchid, minecraft:lily_of_the_valley, minecraft:oxeye_daisy,
            minecraft:red_tulip, minecraft:orange_tulip, minecraft:white_tulip, minecraft:pink_tulip,
            minecraft:sunflower, minecraft:lilac, minecraft:rose_bush, minecraft:peony,
            minecraft:sweet_berry_bush, minecraft:azalea, minecraft:flowering_azalea

            LEAVES:
            minecraft:oak_leaves, minecraft:spruce_leaves, minecraft:birch_leaves,
            minecraft:dark_oak_leaves, minecraft:jungle_leaves, minecraft:acacia_leaves,
            minecraft:cherry_leaves, minecraft:azalea_leaves, minecraft:flowering_azalea_leaves

            POTTED PLANTS (use these instead of separate flower_pot + flower):
            minecraft:potted_poppy, minecraft:potted_dandelion, minecraft:potted_red_tulip,
            minecraft:potted_orange_tulip, minecraft:potted_white_tulip, minecraft:potted_pink_tulip,
            minecraft:potted_blue_orchid, minecraft:potted_allium, minecraft:potted_azure_bluet,
            minecraft:potted_cornflower, minecraft:potted_lily_of_the_valley, minecraft:potted_oxeye_daisy,
            minecraft:potted_oak_sapling, minecraft:potted_spruce_sapling, minecraft:potted_birch_sapling,
            minecraft:potted_dark_oak_sapling, minecraft:potted_fern, minecraft:potted_red_mushroom,
            minecraft:potted_brown_mushroom, minecraft:potted_cactus, minecraft:potted_dead_bush,
            minecraft:potted_azalea_bush, minecraft:potted_flowering_azalea_bush

            DECORATION:
            minecraft:flower_pot, minecraft:ladder, minecraft:iron_bars,
            minecraft:chain, minecraft:lightning_rod, minecraft:bell,
            minecraft:red_carpet, minecraft:white_carpet, minecraft:brown_carpet, minecraft:green_carpet,
            minecraft:blue_carpet, minecraft:black_carpet, minecraft:gray_carpet, minecraft:light_blue_carpet,
            minecraft:light_gray_carpet, minecraft:cyan_carpet, minecraft:lime_carpet, minecraft:magenta_carpet,
            minecraft:orange_carpet, minecraft:pink_carpet, minecraft:purple_carpet, minecraft:yellow_carpet,
            minecraft:white_banner, minecraft:red_banner, minecraft:blue_banner, minecraft:black_banner,
            minecraft:oak_button, minecraft:stone_button, minecraft:lever,
            minecraft:tripwire_hook, minecraft:potted_oak_sapling, minecraft:potted_spruce_sapling

            REDSTONE:
            minecraft:redstone_lamp, minecraft:observer, minecraft:piston, minecraft:sticky_piston

            TERRAIN:
            minecraft:dirt_path, minecraft:gravel, minecraft:sand, minecraft:red_sand,
            minecraft:coarse_dirt, minecraft:rooted_dirt, minecraft:mud_bricks

            SPECIAL:
            minecraft:air, minecraft:water, minecraft:cobweb, minecraft:hay_block,
            minecraft:honeycomb_block, minecraft:dried_kelp_block, minecraft:bone_block,
            minecraft:purpur_block, minecraft:purpur_pillar, minecraft:end_stone_bricks

            DO NOT use: item_frame, painting, armor_stand (entities, not blocks)
            DO NOT use: grass (renamed to short_grass), spruce_ladder (does not exist, use ladder)
            DO NOT place flower_pot + flower separately — use potted_poppy, potted_red_tulip, etc.
            DO NOT give "torch" a facing property — only "wall_torch" has facing
            """;
}
