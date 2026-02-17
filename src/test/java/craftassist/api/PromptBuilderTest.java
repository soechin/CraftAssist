package craftassist.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderTest {

    @Test
    void buildPlanningPrompt_returnsNonEmpty() {
        String prompt = PromptBuilder.buildPlanningPrompt();
        assertNotNull(prompt);
        assertFalse(prompt.isBlank());
    }

    @Test
    void buildPlanningPrompt_containsKeyInstructions() {
        String prompt = PromptBuilder.buildPlanningPrompt();
        assertTrue(prompt.contains("CONCEPT"));
        assertTrue(prompt.contains("DIMENSIONS"));
        assertTrue(prompt.contains("MATERIALS"));
        assertTrue(prompt.contains("minecraft:"));
        assertTrue(prompt.contains("ENTRANCE"));
    }

    @Test
    void buildBuildingPrompt_injectsBlueprint() {
        String blueprint = "A small wooden cabin with a fireplace";
        String prompt = PromptBuilder.buildBuildingPrompt(blueprint, 5000);
        assertTrue(prompt.contains(blueprint));
    }

    @Test
    void buildBuildingPrompt_injectsMaxBlocks() {
        String prompt = PromptBuilder.buildBuildingPrompt("test", 12345);
        assertTrue(prompt.contains("12345"));
    }

    @Test
    void buildBuildingPrompt_containsJsonFormatInstructions() {
        String prompt = PromptBuilder.buildBuildingPrompt("test", 1000);
        assertTrue(prompt.contains("regions"));
        assertTrue(prompt.contains("blocks"));
        assertTrue(prompt.contains("hollow"));
        assertTrue(prompt.contains("facing"));
        assertTrue(prompt.contains("COORDINATE SYSTEM"));
    }

    @Test
    void buildBuildingPrompt_containsPropertyReference() {
        String prompt = PromptBuilder.buildBuildingPrompt("test", 1000);
        assertTrue(prompt.contains("half"));
        assertTrue(prompt.contains("type"));
        assertTrue(prompt.contains("hinge"));
        assertTrue(prompt.contains("axis"));
    }
}
