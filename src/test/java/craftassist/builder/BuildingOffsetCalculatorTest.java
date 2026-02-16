package craftassist.builder;

import craftassist.builder.BuildingOffsetCalculator.BoundingBox;
import com.google.gson.Gson;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuildingOffsetCalculatorTest {

    private static final Gson GSON = new Gson();

    // ========== computeBoundingBox 測試 ==========

    @Test
    void computeBoundingBox_singleRegion() {
        String json = """
                {"regions": [{"block": "minecraft:stone", "from": [0, 0, 0], "to": [4, 3, 6]}]}
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        BoundingBox bbox = BuildingOffsetCalculator.computeBoundingBox(structure);

        assertEquals(0, bbox.minX());
        assertEquals(0, bbox.minY());
        assertEquals(0, bbox.minZ());
        assertEquals(4, bbox.maxX());
        assertEquals(3, bbox.maxY());
        assertEquals(6, bbox.maxZ());
        assertEquals(5, bbox.width());
        assertEquals(7, bbox.depth());
    }

    @Test
    void computeBoundingBox_multipleRegions() {
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:stone", "from": [0, 0, 0], "to": [2, 2, 2]},
                    {"block": "minecraft:oak_planks", "from": [5, 0, 5], "to": [8, 4, 8]}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        BoundingBox bbox = BuildingOffsetCalculator.computeBoundingBox(structure);

        assertEquals(0, bbox.minX());
        assertEquals(0, bbox.minY());
        assertEquals(0, bbox.minZ());
        assertEquals(8, bbox.maxX());
        assertEquals(4, bbox.maxY());
        assertEquals(8, bbox.maxZ());
    }

    @Test
    void computeBoundingBox_withBlocks() {
        String json = """
                {
                  "regions": [{"block": "minecraft:stone", "from": [0, 0, 0], "to": [2, 2, 2]}],
                  "blocks": [{"block": "minecraft:torch", "pos": [10, 5, 10]}]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        BoundingBox bbox = BuildingOffsetCalculator.computeBoundingBox(structure);

        assertEquals(0, bbox.minX());
        assertEquals(0, bbox.minY());
        assertEquals(0, bbox.minZ());
        assertEquals(10, bbox.maxX());
        assertEquals(5, bbox.maxY());
        assertEquals(10, bbox.maxZ());
    }

    @Test
    void computeBoundingBox_emptyStructure_returnsZeroBbox() {
        BuildStructure structure = GSON.fromJson("{}", BuildStructure.class);
        BoundingBox bbox = BuildingOffsetCalculator.computeBoundingBox(structure);

        assertEquals(0, bbox.minX());
        assertEquals(0, bbox.minY());
        assertEquals(0, bbox.minZ());
        assertEquals(0, bbox.maxX());
        assertEquals(0, bbox.maxY());
        assertEquals(0, bbox.maxZ());
    }

    @Test
    void computeBoundingBox_negativeCoordinates() {
        String json = """
                {"regions": [{"block": "minecraft:stone", "from": [-3, -1, -5], "to": [3, 4, 5]}]}
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        BoundingBox bbox = BuildingOffsetCalculator.computeBoundingBox(structure);

        assertEquals(-3, bbox.minX());
        assertEquals(-1, bbox.minY());
        assertEquals(-5, bbox.minZ());
        assertEquals(3, bbox.maxX());
        assertEquals(4, bbox.maxY());
        assertEquals(5, bbox.maxZ());
        assertEquals(7, bbox.width());
        assertEquals(11, bbox.depth());
    }

    @Test
    void computeBoundingBox_blocksOnly() {
        String json = """
                {
                  "blocks": [
                    {"block": "minecraft:torch", "pos": [1, 2, 3]},
                    {"block": "minecraft:torch", "pos": [5, 0, 7]}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        BoundingBox bbox = BuildingOffsetCalculator.computeBoundingBox(structure);

        assertEquals(1, bbox.minX());
        assertEquals(0, bbox.minY());
        assertEquals(3, bbox.minZ());
        assertEquals(5, bbox.maxX());
        assertEquals(2, bbox.maxY());
        assertEquals(7, bbox.maxZ());
    }

    @Test
    void computeBoundingBox_invalidRegionSkipped() {
        // region 缺少 from/to 應被跳過
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:stone", "from": [0, 0, 0]},
                    {"block": "minecraft:oak_planks", "from": [1, 1, 1], "to": [3, 3, 3]}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        BoundingBox bbox = BuildingOffsetCalculator.computeBoundingBox(structure);

        assertEquals(1, bbox.minX());
        assertEquals(1, bbox.minY());
        assertEquals(1, bbox.minZ());
        assertEquals(3, bbox.maxX());
        assertEquals(3, bbox.maxY());
        assertEquals(3, bbox.maxZ());
    }

    // ========== computeOrigin 測試 ==========

    @Test
    void computeOrigin_facingNorth() {
        BlockPos playerPos = new BlockPos(100, 64, 100);
        BoundingBox bbox = new BoundingBox(0, 0, 0, 4, 3, 6);
        // width=5, depth=7

        BlockPos origin = BuildingOffsetCalculator.computeOrigin(playerPos, Direction.NORTH, bbox);

        // X: 100 - 0 - 5/2 = 98
        assertEquals(98, origin.getX());
        // Y: 64
        assertEquals(64, origin.getY());
        // Z: 100 - 6 - 2 = 92
        assertEquals(92, origin.getZ());
    }

    @Test
    void computeOrigin_facingSouth() {
        BlockPos playerPos = new BlockPos(100, 64, 100);
        BoundingBox bbox = new BoundingBox(0, 0, 0, 4, 3, 6);

        BlockPos origin = BuildingOffsetCalculator.computeOrigin(playerPos, Direction.SOUTH, bbox);

        // X: 100 - 0 - 5/2 = 98
        assertEquals(98, origin.getX());
        assertEquals(64, origin.getY());
        // Z: 100 - 0 + 2 = 102
        assertEquals(102, origin.getZ());
    }

    @Test
    void computeOrigin_facingEast() {
        BlockPos playerPos = new BlockPos(100, 64, 100);
        BoundingBox bbox = new BoundingBox(0, 0, 0, 4, 3, 6);

        BlockPos origin = BuildingOffsetCalculator.computeOrigin(playerPos, Direction.EAST, bbox);

        // X: 100 - 0 + 2 = 102
        assertEquals(102, origin.getX());
        assertEquals(64, origin.getY());
        // Z: 100 - 0 - 7/2 = 97
        assertEquals(97, origin.getZ());
    }

    @Test
    void computeOrigin_facingWest() {
        BlockPos playerPos = new BlockPos(100, 64, 100);
        BoundingBox bbox = new BoundingBox(0, 0, 0, 4, 3, 6);

        BlockPos origin = BuildingOffsetCalculator.computeOrigin(playerPos, Direction.WEST, bbox);

        // X: 100 - 4 - 2 = 94
        assertEquals(94, origin.getX());
        assertEquals(64, origin.getY());
        // Z: 100 - 0 - 7/2 = 97
        assertEquals(97, origin.getZ());
    }

    @Test
    void computeOrigin_facingUp_returnsPlayerPos() {
        BlockPos playerPos = new BlockPos(100, 64, 100);
        BoundingBox bbox = new BoundingBox(0, 0, 0, 4, 3, 6);

        BlockPos origin = BuildingOffsetCalculator.computeOrigin(playerPos, Direction.UP, bbox);

        assertEquals(playerPos, origin);
    }
}
