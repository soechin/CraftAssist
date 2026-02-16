package craftassist.builder;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuildStructureTest {

    private static final Gson GSON = new Gson();

    @Test
    void deserialize_fullStructure_parsesCorrectly() {
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:stone", "from": [0, 0, 0], "to": [4, 3, 4], "hollow": true, "facing": "north"}
                  ],
                  "blocks": [
                    {"block": "minecraft:oak_door", "pos": [2, 1, 0], "properties": {"facing": "south", "half": "lower"}}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);

        assertNotNull(structure.getRegions());
        assertEquals(1, structure.getRegions().size());

        var region = structure.getRegions().get(0);
        assertEquals("minecraft:stone", region.getBlock());
        assertArrayEquals(new int[]{0, 0, 0}, region.getFrom());
        assertArrayEquals(new int[]{4, 3, 4}, region.getTo());
        assertTrue(region.isHollow());
        assertEquals("north", region.getFacing());

        assertNotNull(structure.getBlocks());
        assertEquals(1, structure.getBlocks().size());

        var block = structure.getBlocks().get(0);
        assertEquals("minecraft:oak_door", block.getBlock());
        assertArrayEquals(new int[]{2, 1, 0}, block.getPos());
        assertNotNull(block.getProperties());
        assertEquals("south", block.getProperties().get("facing"));
        assertEquals("lower", block.getProperties().get("half"));
    }

    @Test
    void deserialize_emptyJson_fieldsAreNull() {
        BuildStructure structure = GSON.fromJson("{}", BuildStructure.class);
        assertNull(structure.getRegions());
        assertNull(structure.getBlocks());
    }

    @Test
    void deserialize_regionsOnly_blocksIsNull() {
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:oak_planks", "from": [0, 0, 0], "to": [2, 2, 2], "hollow": false}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        assertNotNull(structure.getRegions());
        assertEquals(1, structure.getRegions().size());
        assertNull(structure.getBlocks());
    }

    @Test
    void deserialize_blocksOnly_regionsIsNull() {
        String json = """
                {
                  "blocks": [
                    {"block": "minecraft:torch", "pos": [1, 2, 3]}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        assertNull(structure.getRegions());
        assertNotNull(structure.getBlocks());
        assertEquals(1, structure.getBlocks().size());
    }

    @Test
    void deserialize_blockWithoutProperties_propertiesIsNull() {
        String json = """
                {
                  "blocks": [
                    {"block": "minecraft:stone", "pos": [0, 0, 0]}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        var block = structure.getBlocks().get(0);
        assertNull(block.getProperties());
    }

    @Test
    void deserialize_regionWithoutFacing_facingIsNull() {
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:stone", "from": [0, 0, 0], "to": [1, 1, 1], "hollow": false}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        var region = structure.getRegions().get(0);
        assertNull(region.getFacing());
        assertFalse(region.isHollow());
    }

    @Test
    void deserialize_multipleRegions_allParsed() {
        String json = """
                {
                  "regions": [
                    {"block": "minecraft:stone", "from": [0, 0, 0], "to": [4, 0, 4], "hollow": false},
                    {"block": "minecraft:oak_planks", "from": [0, 1, 0], "to": [4, 1, 4], "hollow": false},
                    {"block": "minecraft:glass", "from": [1, 2, 1], "to": [3, 3, 3], "hollow": true}
                  ]
                }
                """;
        BuildStructure structure = GSON.fromJson(json, BuildStructure.class);
        assertEquals(3, structure.getRegions().size());
        assertEquals("minecraft:stone", structure.getRegions().get(0).getBlock());
        assertEquals("minecraft:oak_planks", structure.getRegions().get(1).getBlock());
        assertEquals("minecraft:glass", structure.getRegions().get(2).getBlock());
    }
}
