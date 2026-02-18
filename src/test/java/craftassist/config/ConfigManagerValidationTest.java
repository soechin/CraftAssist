package craftassist.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerValidationTest {

    private ModConfig defaultConfig() {
        return new ModConfig();
    }

    @Test
    void defaults_passValidation_noFixNeeded() {
        ModConfig config = defaultConfig();
        boolean fixed = ConfigManager.validateAndFix(config);
        assertFalse(fixed);
    }

    @Test
    void blocksPerTick_zero_resetsTo500() {
        ModConfig config = defaultConfig();
        config.setBlocksPerTick(0);

        boolean fixed = ConfigManager.validateAndFix(config);

        assertTrue(fixed);
        assertEquals(500, config.getBlocksPerTick());
    }

    @Test
    void blocksPerTick_negative_resetsTo500() {
        ModConfig config = defaultConfig();
        config.setBlocksPerTick(-10);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(500, config.getBlocksPerTick());
    }

    @Test
    void maxBlocks_overLimit_clampedTo1M() {
        ModConfig config = defaultConfig();
        config.setMaxBlocks(2_000_000);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(1_000_000, config.getMaxBlocks());
    }

    @Test
    void maxBlocks_atLimit_noFix() {
        ModConfig config = defaultConfig();
        config.setMaxBlocks(1_000_000);

        assertFalse(ConfigManager.validateAndFix(config));
    }

    @Test
    void timeoutSeconds_tooLow_resetsTo60() {
        ModConfig config = defaultConfig();
        config.setTimeoutSeconds(5);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(60, config.getTimeoutSeconds());
    }

    @Test
    void timeoutSeconds_tooHigh_resetsTo60() {
        ModConfig config = defaultConfig();
        config.setTimeoutSeconds(500);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(60, config.getTimeoutSeconds());
    }

    @Test
    void rateLimitTokens_zero_resetsTo3() {
        ModConfig config = defaultConfig();
        config.setRateLimitTokens(0);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(3, config.getRateLimitTokens());
    }

    @Test
    void rateLimitTokens_negative_resetsTo3() {
        ModConfig config = defaultConfig();
        config.setRateLimitTokens(-5);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(3, config.getRateLimitTokens());
    }

    @Test
    void rateLimitRefillSeconds_zero_resetsTo60() {
        ModConfig config = defaultConfig();
        config.setRateLimitRefillSeconds(0);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(60, config.getRateLimitRefillSeconds());
    }

    @Test
    void rateLimitRefillSeconds_overLimit_resetsTo60() {
        ModConfig config = defaultConfig();
        config.setRateLimitRefillSeconds(5000);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(60, config.getRateLimitRefillSeconds());
    }

    @Test
    void maxRetries_negative_resetsTo2() {
        ModConfig config = defaultConfig();
        config.setMaxRetries(-1);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(2, config.getMaxRetries());
    }

    @Test
    void maxRetries_overLimit_resetsTo2() {
        ModConfig config = defaultConfig();
        config.setMaxRetries(11);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(2, config.getMaxRetries());
    }

    @Test
    void maxCoordinate_zero_resetsTo200() {
        ModConfig config = defaultConfig();
        config.setMaxCoordinate(0);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(200, config.getMaxCoordinate());
    }

    @Test
    void maxCoordinate_overLimit_resetsTo200() {
        ModConfig config = defaultConfig();
        config.setMaxCoordinate(2000);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(200, config.getMaxCoordinate());
    }

    @Test
    void maxRegionVolume_zero_resetsTo100000() {
        ModConfig config = defaultConfig();
        config.setMaxRegionVolume(0);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(100_000, config.getMaxRegionVolume());
    }

    @Test
    void maxRegionVolume_overLimit_resetsTo100000() {
        ModConfig config = defaultConfig();
        config.setMaxRegionVolume(2_000_000);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(100_000, config.getMaxRegionVolume());
    }

    @Test
    void multipleInvalidFields_allFixed() {
        ModConfig config = defaultConfig();
        config.setBlocksPerTick(0);
        config.setRateLimitTokens(-1);
        config.setMaxRetries(99);
        config.setMaxCoordinate(0);

        assertTrue(ConfigManager.validateAndFix(config));
        assertEquals(500, config.getBlocksPerTick());
        assertEquals(3, config.getRateLimitTokens());
        assertEquals(2, config.getMaxRetries());
        assertEquals(200, config.getMaxCoordinate());
    }
}
