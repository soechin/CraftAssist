package craftassist.builder;

import com.google.gson.Gson;
import craftassist.config.ModConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class BuildStructureValidatorTest {

    private static final Gson GSON = new Gson();

    /** 注入的假方塊驗證器，只接受白名單內的方塊 */
    private static final Set<String> VALID_BLOCKS = Set.of(
            "minecraft:stone", "minecraft:oak_planks", "minecraft:oak_door",
            "minecraft:glass", "minecraft:spruce_log"
    );
    private static final Predicate<String> BLOCK_VALIDATOR = VALID_BLOCKS::contains;

    private ModConfig config;

    @BeforeEach
    void setUp() {
        config = new ModConfig();
        config.setMaxCoordinate(200);
        config.setMaxRegionVolume(100_000);
    }

    private BuildStructure parse(String json) {
        return GSON.fromJson(json, BuildStructure.class);
    }

    @Test
    void validStructure_noIssues() {
        BuildStructure structure = parse("""
                {
                  "regions": [{"block":"minecraft:stone","from":[0,0,0],"to":[4,3,4],"hollow":false}],
                  "blocks": [{"block":"minecraft:oak_door","pos":[2,1,0]}]
                }
                """);

        var result = BuildStructureValidator.validate(structure, config, BLOCK_VALIDATOR);

        assertFalse(result.hasIssues());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    void invalidBlockId_reportedAsIssue() {
        BuildStructure structure = parse("""
                {
                  "regions": [{"block":"minecraft:invalid_block","from":[0,0,0],"to":[4,3,4],"hollow":false}]
                }
                """);

        var result = BuildStructureValidator.validate(structure, config, BLOCK_VALIDATOR);

        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().stream()
                .anyMatch(s -> s.contains("無效的方塊 ID")));
    }

    @Test
    void coordinateExceedsMaxCoordinate_reportedAsIssue() {
        config.setMaxCoordinate(50);
        BuildStructure structure = parse("""
                {
                  "regions": [{"block":"minecraft:stone","from":[0,0,0],"to":[100,3,4],"hollow":false}]
                }
                """);

        var result = BuildStructureValidator.validate(structure, config, BLOCK_VALIDATOR);

        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().stream()
                .anyMatch(s -> s.contains("超出範圍")));
    }

    @Test
    void regionVolumeExceedsMax_reportedAsIssue() {
        config.setMaxRegionVolume(100);
        BuildStructure structure = parse("""
                {
                  "regions": [{"block":"minecraft:stone","from":[0,0,0],"to":[10,10,10],"hollow":false}]
                }
                """);

        var result = BuildStructureValidator.validate(structure, config, BLOCK_VALIDATOR);

        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().stream()
                .anyMatch(s -> s.contains("體積")));
    }

    @Test
    void missingFromArray_reportedAsIssue() {
        BuildStructure structure = parse("""
                {
                  "regions": [{"block":"minecraft:stone","to":[4,3,4],"hollow":false}]
                }
                """);

        var result = BuildStructureValidator.validate(structure, config, BLOCK_VALIDATOR);

        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().stream()
                .anyMatch(s -> s.contains("'from'")));
    }

    @Test
    void missingToArray_reportedAsIssue() {
        BuildStructure structure = parse("""
                {
                  "regions": [{"block":"minecraft:stone","from":[0,0,0],"hollow":false}]
                }
                """);

        var result = BuildStructureValidator.validate(structure, config, BLOCK_VALIDATOR);

        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().stream()
                .anyMatch(s -> s.contains("'to'")));
    }

    @Test
    void blockWithInvalidPos_reportedAsIssue() {
        BuildStructure structure = parse("""
                {
                  "blocks": [{"block":"minecraft:stone"}]
                }
                """);

        var result = BuildStructureValidator.validate(structure, config, BLOCK_VALIDATOR);

        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().stream()
                .anyMatch(s -> s.contains("'pos'")));
    }

    @Test
    void blockPosExceedsMaxCoordinate_reportedAsIssue() {
        config.setMaxCoordinate(10);
        BuildStructure structure = parse("""
                {
                  "blocks": [{"block":"minecraft:glass","pos":[100,2,3]}]
                }
                """);

        var result = BuildStructureValidator.validate(structure, config, BLOCK_VALIDATOR);

        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().stream()
                .anyMatch(s -> s.contains("超出範圍")));
    }

    @Test
    void getReport_containsAllIssues_formatted() {
        BuildStructure structure = parse("""
                {
                  "regions": [
                    {"block":"minecraft:invalid","from":[0,0,0],"to":[4,3,4],"hollow":false},
                    {"block":"minecraft:stone","from":[300,0,0],"to":[4,3,4],"hollow":false}
                  ]
                }
                """);

        var result = BuildStructureValidator.validate(structure, config, BLOCK_VALIDATOR);

        assertTrue(result.hasIssues());
        String report = result.getReport();
        assertTrue(report.contains("1."));
        assertTrue(report.contains("2."));
    }

    @Test
    void multipleIssues_allReported() {
        BuildStructure structure = parse("""
                {
                  "regions": [{"block":"bad_block","from":[500,0,0],"to":[4,3,4],"hollow":false}],
                  "blocks": [{"block":"another_bad","pos":[999,0,0]}]
                }
                """);

        var result = BuildStructureValidator.validate(structure, config, BLOCK_VALIDATOR);

        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().size() >= 2);
    }
}
