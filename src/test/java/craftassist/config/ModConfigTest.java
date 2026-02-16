package craftassist.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModConfigTest {

    @Test
    void defaults_apiKeyIsEmpty() {
        var config = new ModConfig();
        assertEquals("", config.getApiKey());
    }

    @Test
    void defaults_modelIsClaudeSonnet() {
        var config = new ModConfig();
        assertEquals("anthropic/claude-sonnet-4-5", config.getModel());
    }

    @Test
    void defaults_numericFieldsHaveExpectedValues() {
        var config = new ModConfig();
        assertEquals(1_000_000, config.getMaxBlocks());
        assertEquals(500, config.getBlocksPerTick());
        assertEquals(60, config.getTimeoutSeconds());
        assertEquals(100_000, config.getMaxRegionVolume());
        assertEquals(200, config.getMaxCoordinate());
        assertEquals(3, config.getRateLimitTokens());
        assertEquals(60, config.getRateLimitRefillSeconds());
        assertEquals(2, config.getMaxRetries());
    }

    @Test
    void setters_updateValues() {
        var config = new ModConfig();

        config.setApiKey("test-key");
        assertEquals("test-key", config.getApiKey());

        config.setModel("openai/gpt-4");
        assertEquals("openai/gpt-4", config.getModel());

        config.setMaxBlocks(500);
        assertEquals(500, config.getMaxBlocks());

        config.setBlocksPerTick(100);
        assertEquals(100, config.getBlocksPerTick());

        config.setTimeoutSeconds(120);
        assertEquals(120, config.getTimeoutSeconds());

        config.setMaxRegionVolume(50000);
        assertEquals(50000, config.getMaxRegionVolume());

        config.setMaxCoordinate(500);
        assertEquals(500, config.getMaxCoordinate());

        config.setRateLimitTokens(10);
        assertEquals(10, config.getRateLimitTokens());

        config.setRateLimitRefillSeconds(30);
        assertEquals(30, config.getRateLimitRefillSeconds());

        config.setMaxRetries(5);
        assertEquals(5, config.getMaxRetries());
    }
}
