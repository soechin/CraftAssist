package craftassist.builder;

import com.google.gson.Gson;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class BuildStructureRotatorTest {

    private static final Gson GSON = new Gson();

    // ==================== detectEntranceWall 測試 ====================

    @Test
    void detectEntranceWall_doorOnSouthWall_returnsSouth() {
        // 7x5x9 的房子，門在南牆 (Z=8)
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0,0,0], "to": [6,4,8]}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [3,1,8], "properties": {"facing": "north", "half": "lower"}},
                    {"block": "minecraft:oak_door", "pos": [3,2,8], "properties": {"facing": "north", "half": "upper"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        assertEquals(Direction.SOUTH, BuildStructureRotator.detectEntranceWall(structure));
    }

    @Test
    void detectEntranceWall_doorOnNorthWall_returnsNorth() {
        // 門在北牆 (Z=0)
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0,0,0], "to": [6,4,8]}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [3,1,0], "properties": {"facing": "south", "half": "lower"}},
                    {"block": "minecraft:oak_door", "pos": [3,2,0], "properties": {"facing": "south", "half": "upper"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        assertEquals(Direction.NORTH, BuildStructureRotator.detectEntranceWall(structure));
    }

    @Test
    void detectEntranceWall_doorOnEastWall_returnsEast() {
        // 門在東牆 (X=6)
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0,0,0], "to": [6,4,8]}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [6,1,4], "properties": {"facing": "west", "half": "lower"}},
                    {"block": "minecraft:oak_door", "pos": [6,2,4], "properties": {"facing": "west", "half": "upper"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        assertEquals(Direction.EAST, BuildStructureRotator.detectEntranceWall(structure));
    }

    @Test
    void detectEntranceWall_doorOnWestWall_returnsWest() {
        // 門在西牆 (X=0)
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0,0,0], "to": [6,4,8]}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [0,1,4], "properties": {"facing": "east", "half": "lower"}},
                    {"block": "minecraft:oak_door", "pos": [0,2,4], "properties": {"facing": "east", "half": "upper"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        assertEquals(Direction.WEST, BuildStructureRotator.detectEntranceWall(structure));
    }

    @Test
    void detectEntranceWall_noDoor_returnsNull() {
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0,0,0], "to": [6,4,8]}
                  ],
                  "blocks": [
                    {"block": "minecraft:torch", "pos": [3,3,0]}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        assertNull(BuildStructureRotator.detectEntranceWall(structure));
    }

    @Test
    void detectEntranceWall_noBlocks_returnsNull() {
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0,0,0], "to": [6,4,8]}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        assertNull(BuildStructureRotator.detectEntranceWall(structure));
    }

    @Test
    void detectEntranceWall_inconsistentFacing_usesCoordinates() {
        // LLM 常見錯誤：門在北牆 (Z=0) 但 facing=west（不一致）
        // 應以座標判定為 NORTH，而非被 facing 誤導
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0,0,0], "to": [6,4,8]}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [3,1,0], "properties": {"facing": "west", "half": "lower"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        assertEquals(Direction.NORTH, BuildStructureRotator.detectEntranceWall(structure));
    }

    @Test
    void detectEntranceWall_exteriorDecorationsInBlocks_notAffected() {
        // blocks 中有路徑延伸到 regions bbox 之外，不影響判斷
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0,0,0], "to": [6,4,8]}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [3,1,8], "properties": {"facing": "north", "half": "lower"}},
                    {"block": "minecraft:dirt_path", "pos": [3,0,12]},
                    {"block": "minecraft:dirt_path", "pos": [3,0,14]}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        // 門在 Z=8 = regionMaxZ → 南牆
        assertEquals(Direction.SOUTH, BuildStructureRotator.detectEntranceWall(structure));
    }

    @Test
    void detectEntranceWall_multipleDoorsOnSameWall_returnsThatWall() {
        // 3 個門都在南牆
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0,0,0], "to": [10,4,8]}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [2,1,8], "properties": {"facing": "north", "half": "lower"}},
                    {"block": "minecraft:oak_door", "pos": [5,1,8], "properties": {"facing": "north", "half": "lower"}},
                    {"block": "minecraft:oak_door", "pos": [8,1,8], "properties": {"facing": "north", "half": "lower"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        assertEquals(Direction.SOUTH, BuildStructureRotator.detectEntranceWall(structure));
    }

    @Test
    void detectEntranceWall_doorsOnDifferentWalls_votingWins() {
        // 2 門在南牆 + 1 門在東牆 → 南牆投票勝出 (2 vs 1)
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0,0,0], "to": [8,4,8]}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [2,1,8], "properties": {"facing": "north", "half": "lower"}},
                    {"block": "minecraft:oak_door", "pos": [6,1,8], "properties": {"facing": "north", "half": "lower"}},
                    {"block": "minecraft:oak_door", "pos": [8,1,4], "properties": {"facing": "west", "half": "lower"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        assertEquals(Direction.SOUTH, BuildStructureRotator.detectEntranceWall(structure));
    }

    @Test
    void detectEntranceWall_hospitalScenario_bigDoorWins() {
        // 醫院情境：大門 2 扇在南牆 (Z=17) + 小門 1 扇在東牆 (X=15)
        // 平均座標會被拉偏，但投票法正確選出南牆
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:quartz_block", "from": [-1,-1,0], "to": [15,6,17]}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [5,1,17], "properties": {"facing": "north", "half": "lower"}},
                    {"block": "minecraft:oak_door", "pos": [9,1,17], "properties": {"facing": "north", "half": "lower"}},
                    {"block": "minecraft:iron_door", "pos": [15,1,9], "properties": {"facing": "west", "half": "lower"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        // 投票：南牆 2 票，東牆 1 票 → 南牆
        assertEquals(Direction.SOUTH, BuildStructureRotator.detectEntranceWall(structure));
    }

    @Test
    void detectEntranceWall_trapdoorIgnored() {
        // trapdoor 不算門
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0,0,0], "to": [6,4,8]}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_trapdoor", "pos": [3,3,0], "properties": {"facing": "south", "half": "bottom"}},
                    {"block": "minecraft:oak_door", "pos": [3,1,8], "properties": {"facing": "north", "half": "lower"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        // 只有 oak_door 在南牆
        assertEquals(Direction.SOUTH, BuildStructureRotator.detectEntranceWall(structure));
    }

    // ==================== computeRotationCount 測試 ====================

    @ParameterizedTest(name = "entrance={0}, playerFacing={1} → rotations={2}")
    @CsvSource({
            // 入口已在正確位置 → 0 次
            "SOUTH, NORTH, 0",
            "NORTH, SOUTH, 0",
            "WEST,  EAST,  0",
            "EAST,  WEST,  0",
            // 入口在對面 → 2 次
            "NORTH, NORTH, 2",
            "SOUTH, SOUTH, 2",
            "EAST,  EAST,  2",
            "WEST,  WEST,  2",
            // 入口在右側 → 1 次 CW
            "EAST,  NORTH, 1",
            "SOUTH, EAST,  1",
            "WEST,  SOUTH, 1",
            "NORTH, WEST,  1",
            // 入口在左側 → 3 次 CW
            "WEST,  NORTH, 3",
            "NORTH, EAST,  3",
            "EAST,  SOUTH, 3",
            "SOUTH, WEST,  3"
    })
    void computeRotationCount(Direction entranceWall, Direction playerFacing, int expected) {
        assertEquals(expected,
                BuildStructureRotator.computeRotationCount(entranceWall, playerFacing));
    }

    // ==================== rotateFacingCW 測試 ====================

    @Test
    void rotateFacingCW_oneTurn() {
        assertEquals("west", BuildStructureRotator.rotateFacingCW("south", 1));
        assertEquals("north", BuildStructureRotator.rotateFacingCW("west", 1));
        assertEquals("east", BuildStructureRotator.rotateFacingCW("north", 1));
        assertEquals("south", BuildStructureRotator.rotateFacingCW("east", 1));
    }

    @Test
    void rotateFacingCW_twoTurns() {
        assertEquals("north", BuildStructureRotator.rotateFacingCW("south", 2));
        assertEquals("south", BuildStructureRotator.rotateFacingCW("north", 2));
    }

    @Test
    void rotateFacingCW_fourTurnsReturnsOriginal() {
        assertEquals("north", BuildStructureRotator.rotateFacingCW("north", 4));
    }

    @Test
    void rotateFacingCW_upDown_unchanged() {
        assertEquals("up", BuildStructureRotator.rotateFacingCW("up", 1));
        assertEquals("down", BuildStructureRotator.rotateFacingCW("down", 3));
    }

    @Test
    void rotateFacingCW_null_returnsNull() {
        assertNull(BuildStructureRotator.rotateFacingCW(null, 1));
    }

    // ==================== rotateAxis 測試 ====================

    @Test
    void rotateAxis_oddTurns_xzSwap() {
        assertEquals("z", BuildStructureRotator.rotateAxis("x", 1));
        assertEquals("x", BuildStructureRotator.rotateAxis("z", 1));
        assertEquals("z", BuildStructureRotator.rotateAxis("x", 3));
    }

    @Test
    void rotateAxis_evenTurns_unchanged() {
        assertEquals("x", BuildStructureRotator.rotateAxis("x", 2));
        assertEquals("z", BuildStructureRotator.rotateAxis("z", 0));
    }

    @Test
    void rotateAxis_y_alwaysUnchanged() {
        assertEquals("y", BuildStructureRotator.rotateAxis("y", 1));
        assertEquals("y", BuildStructureRotator.rotateAxis("y", 3));
    }

    @Test
    void rotateAxis_null_returnsNull() {
        assertNull(BuildStructureRotator.rotateAxis(null, 1));
    }

    // ==================== rotateStructure 測試 ====================

    @Test
    void rotateStructure_zeroRotations_unchanged() {
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:stone", "from": [0,0,0], "to": [4,3,6]}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [2,1,6], "properties": {"facing": "north", "half": "lower"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        BuildStructureRotator.rotateStructure(structure, 0);

        assertArrayEquals(new int[]{0, 0, 0}, structure.getRegions().get(0).getFrom());
        assertArrayEquals(new int[]{4, 3, 6}, structure.getRegions().get(0).getTo());
        assertArrayEquals(new int[]{2, 1, 6}, structure.getBlocks().get(0).getPos());
        assertEquals("north", structure.getBlocks().get(0).getProperties().get("facing"));
    }

    @Test
    void rotateStructure_oneCW_coordsAndFacingCorrect() {
        // bbox: (0,0,0)-(4,3,6), maxZ=6
        // CW90: (x,y,z) → (6-z, y, x)
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:stone", "from": [0,0,0], "to": [4,3,6], "facing": "north"}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [2,1,6], "properties": {"facing": "north", "half": "lower"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        BuildStructureRotator.rotateStructure(structure, 1);

        // region from (0,0,0) → (6-0, 0, 0) = (6,0,0)
        // region to (4,3,6) → (6-6, 3, 4) = (0,3,4)
        assertArrayEquals(new int[]{6, 0, 0}, structure.getRegions().get(0).getFrom());
        assertArrayEquals(new int[]{0, 3, 4}, structure.getRegions().get(0).getTo());
        assertEquals("east", structure.getRegions().get(0).getFacing());

        // block pos (2,1,6) → (6-6, 1, 2) = (0,1,2)
        assertArrayEquals(new int[]{0, 1, 2}, structure.getBlocks().get(0).getPos());
        assertEquals("east", structure.getBlocks().get(0).getProperties().get("facing"));
        assertEquals("lower", structure.getBlocks().get(0).getProperties().get("half"));
    }

    @Test
    void rotateStructure_fourRotations_restoresOriginal() {
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:stone", "from": [0,0,0], "to": [4,3,6], "facing": "north"}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [2,1,0], "properties": {"facing": "south", "half": "lower", "axis": "x"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);

        // 先保存原值
        int[] origFrom = structure.getRegions().get(0).getFrom().clone();
        int[] origTo = structure.getRegions().get(0).getTo().clone();
        int[] origPos = structure.getBlocks().get(0).getPos().clone();

        BuildStructureRotator.rotateStructure(structure, 4);

        assertArrayEquals(origFrom, structure.getRegions().get(0).getFrom());
        assertArrayEquals(origTo, structure.getRegions().get(0).getTo());
        assertArrayEquals(origPos, structure.getBlocks().get(0).getPos());
        assertEquals("north", structure.getRegions().get(0).getFacing());
        assertEquals("south", structure.getBlocks().get(0).getProperties().get("facing"));
        assertEquals("x", structure.getBlocks().get(0).getProperties().get("axis"));
    }

    @Test
    void rotateStructure_axisProperty_swapsXZ() {
        String json = """
                {
                  "blocks": [
                    {"block": "minecraft:oak_log", "pos": [0,0,0], "properties": {"axis": "x"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        BuildStructureRotator.rotateStructure(structure, 1);
        assertEquals("z", structure.getBlocks().get(0).getProperties().get("axis"));
    }

    @Test
    void rotateStructure_halfProperty_unchanged() {
        String json = """
                {
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [0,0,0], "properties": {"facing": "north", "half": "upper"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        BuildStructureRotator.rotateStructure(structure, 1);
        assertEquals("upper", structure.getBlocks().get(0).getProperties().get("half"));
    }

    // ==================== 端到端整合測試 ====================

    /**
     * 建構標準測試房子：7x5x9 (W=7, H=5, D=9)
     * @param doorWall 門在哪面牆 ("north", "south", "east", "west")
     */
    private BuildStructure buildTestHouse(String doorWall) {
        int W = 6, D = 8; // maxX=6, maxZ=8

        // 門的位置和 facing 根據牆面
        int doorX, doorZ;
        String doorFacing;
        switch (doorWall) {
            case "north" -> { doorX = 3; doorZ = 0; doorFacing = "south"; }
            case "south" -> { doorX = 3; doorZ = D; doorFacing = "north"; }
            case "east"  -> { doorX = W; doorZ = 4; doorFacing = "west"; }
            case "west"  -> { doorX = 0; doorZ = 4; doorFacing = "east"; }
            default -> throw new IllegalArgumentException("Invalid wall: " + doorWall);
        }

        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0,0,0], "to": [%d,0,%d]},
                    {"block": "minecraft:oak_planks", "from": [0,1,0], "to": [%d,4,0]},
                    {"block": "minecraft:oak_planks", "from": [0,1,%d], "to": [%d,4,%d]},
                    {"block": "minecraft:oak_planks", "from": [0,1,0], "to": [0,4,%d]},
                    {"block": "minecraft:oak_planks", "from": [%d,1,0], "to": [%d,4,%d]}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [%d,1,%d], "properties": {"facing": "%s", "half": "lower"}},
                    {"block": "minecraft:oak_door", "pos": [%d,2,%d], "properties": {"facing": "%s", "half": "upper"}}
                  ]
                }
                """.formatted(
                W, D,           // floor
                W,              // north wall
                D, W, D,        // south wall
                D,              // west wall
                W, W, D,        // east wall
                doorX, doorZ, doorFacing,  // door lower
                doorX, doorZ, doorFacing   // door upper
        );
        return GSON.fromJson(json, BuildStructure.class);
    }

    @Test
    void endToEnd_doorOnNorth_playerFacingEast_buildingInFront() {
        BuildStructure structure = buildTestHouse("north");
        Direction playerFacing = Direction.EAST;
        BlockPos playerPos = new BlockPos(100, 64, 100);

        // 偵測 + 旋轉
        Direction entranceWall = BuildStructureRotator.detectEntranceWall(structure);
        assertEquals(Direction.NORTH, entranceWall);

        int rotations = BuildStructureRotator.computeRotationCount(entranceWall, playerFacing);
        assertEquals(3, rotations); // NORTH → WEST 需要 3 次 CW

        BuildStructureRotator.rotateStructure(structure, rotations);

        // 旋轉後入口應在西牆 (playerFacing.getOpposite() = WEST)
        Direction newEntrance = BuildStructureRotator.detectEntranceWall(structure);
        assertEquals(Direction.WEST, newEntrance);

        // 計算 origin
        BuildingOffsetCalculator.BoundingBox bbox =
                BuildingOffsetCalculator.computeBoundingBox(structure);
        BlockPos origin = BuildingOffsetCalculator.computeOrigin(playerPos, playerFacing, bbox);

        // 建築應在玩家東方（前方），不是西方（背後）
        assertTrue(origin.getX() > playerPos.getX(),
                "建築 origin.x 應大於 playerPos.x（在玩家東方/前方），實際 origin.x=" + origin.getX());
    }

    @Test
    void endToEnd_doorOnSouth_playerFacingNorth_noRotation() {
        BuildStructure structure = buildTestHouse("south");
        Direction playerFacing = Direction.NORTH;
        BlockPos playerPos = new BlockPos(100, 64, 100);

        Direction entranceWall = BuildStructureRotator.detectEntranceWall(structure);
        assertEquals(Direction.SOUTH, entranceWall);

        int rotations = BuildStructureRotator.computeRotationCount(entranceWall, playerFacing);
        assertEquals(0, rotations); // 已經正確

        // 不旋轉，直接計算 origin
        BuildingOffsetCalculator.BoundingBox bbox =
                BuildingOffsetCalculator.computeBoundingBox(structure);
        BlockPos origin = BuildingOffsetCalculator.computeOrigin(playerPos, playerFacing, bbox);

        // 建築應在玩家北方（前方）
        assertTrue(origin.getZ() < playerPos.getZ(),
                "建築 origin.z 應小於 playerPos.z（在玩家北方/前方），實際 origin.z=" + origin.getZ());
    }

    @Test
    void endToEnd_doorOnEast_playerFacingSouth_buildingInFront() {
        BuildStructure structure = buildTestHouse("east");
        Direction playerFacing = Direction.SOUTH;
        BlockPos playerPos = new BlockPos(100, 64, 100);

        Direction entranceWall = BuildStructureRotator.detectEntranceWall(structure);
        assertEquals(Direction.EAST, entranceWall);

        int rotations = BuildStructureRotator.computeRotationCount(entranceWall, playerFacing);
        BuildStructureRotator.rotateStructure(structure, rotations);

        // 旋轉後入口應在北牆
        Direction newEntrance = BuildStructureRotator.detectEntranceWall(structure);
        assertEquals(Direction.NORTH, newEntrance);

        BuildingOffsetCalculator.BoundingBox bbox =
                BuildingOffsetCalculator.computeBoundingBox(structure);
        BlockPos origin = BuildingOffsetCalculator.computeOrigin(playerPos, playerFacing, bbox);

        // 建築應在玩家南方（前方）
        assertTrue(origin.getZ() > playerPos.getZ(),
                "建築 origin.z 應大於 playerPos.z（在玩家南方/前方），實際 origin.z=" + origin.getZ());
    }

    @Test
    void endToEnd_doorOnWest_playerFacingWest_buildingInFront() {
        BuildStructure structure = buildTestHouse("west");
        Direction playerFacing = Direction.WEST;
        BlockPos playerPos = new BlockPos(100, 64, 100);

        Direction entranceWall = BuildStructureRotator.detectEntranceWall(structure);
        assertEquals(Direction.WEST, entranceWall);

        int rotations = BuildStructureRotator.computeRotationCount(entranceWall, playerFacing);
        BuildStructureRotator.rotateStructure(structure, rotations);

        // 旋轉後入口應在東牆
        Direction newEntrance = BuildStructureRotator.detectEntranceWall(structure);
        assertEquals(Direction.EAST, newEntrance);

        BuildingOffsetCalculator.BoundingBox bbox =
                BuildingOffsetCalculator.computeBoundingBox(structure);
        BlockPos origin = BuildingOffsetCalculator.computeOrigin(playerPos, playerFacing, bbox);

        // 建築應在玩家西方（前方）
        assertTrue(origin.getX() < playerPos.getX(),
                "建築 origin.x 應小於 playerPos.x（在玩家西方/前方），實際 origin.x=" + origin.getX());
    }

    @Test
    void endToEnd_doorWorldPosition_facesPlayer() {
        // 驗證門的相對座標確實在旋轉後的正確牆面
        BuildStructure structure = buildTestHouse("north");
        Direction playerFacing = Direction.EAST;

        Direction entranceWall = BuildStructureRotator.detectEntranceWall(structure);
        int rotations = BuildStructureRotator.computeRotationCount(entranceWall, playerFacing);
        BuildStructureRotator.rotateStructure(structure, rotations);

        BuildingOffsetCalculator.BoundingBox bbox =
                BuildingOffsetCalculator.computeBoundingBox(structure);

        // 找到旋轉後的門座標
        int[] doorPos = null;
        for (BuildStructure.IndividualBlock block : structure.getBlocks()) {
            if (block.getBlock().contains("_door")) {
                var props = block.getProperties();
                if (props != null && "lower".equals(props.get("half"))) {
                    doorPos = block.getPos();
                    break;
                }
            }
        }
        assertNotNull(doorPos, "應找到旋轉後的門方塊");

        // 門在玩家和建築之間（X 座標在 playerPos 和建築遠端之間）
        // 玩家面向東 → 門 X 應該是建築最西面（最靠近玩家的面）
        assertEquals(bbox.minX(), doorPos[0],
                "旋轉後門應在 bbox 的 minX（西牆 = 面對玩家那面）");
    }
}
