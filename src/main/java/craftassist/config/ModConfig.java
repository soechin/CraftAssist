package craftassist.config;

public class ModConfig {
    private String apiKey = "";
    private String model = "anthropic/claude-sonnet-4-5";
    private int maxBlocks = 1000000;
    private int blocksPerTick = 500;
    private int timeoutSeconds = 60;
    private int maxRegionVolume = 100000;
    private int maxCoordinate = 200;
    private int rateLimitTokens = 3;
    private int rateLimitRefillSeconds = 60;
    private int maxRetries = 2;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxBlocks() {
        return maxBlocks;
    }

    public void setMaxBlocks(int maxBlocks) {
        this.maxBlocks = maxBlocks;
    }

    public int getBlocksPerTick() {
        return blocksPerTick;
    }

    public void setBlocksPerTick(int blocksPerTick) {
        this.blocksPerTick = blocksPerTick;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxRegionVolume() {
        return maxRegionVolume;
    }

    public void setMaxRegionVolume(int maxRegionVolume) {
        this.maxRegionVolume = maxRegionVolume;
    }

    public int getMaxCoordinate() {
        return maxCoordinate;
    }

    public void setMaxCoordinate(int maxCoordinate) {
        this.maxCoordinate = maxCoordinate;
    }

    public int getRateLimitTokens() {
        return rateLimitTokens;
    }

    public void setRateLimitTokens(int rateLimitTokens) {
        this.rateLimitTokens = rateLimitTokens;
    }

    public int getRateLimitRefillSeconds() {
        return rateLimitRefillSeconds;
    }

    public void setRateLimitRefillSeconds(int rateLimitRefillSeconds) {
        this.rateLimitRefillSeconds = rateLimitRefillSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}
