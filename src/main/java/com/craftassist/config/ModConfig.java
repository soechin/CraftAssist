package com.craftassist.config;

public class ModConfig {
    private String apiKey = "";
    private String model = "anthropic/claude-sonnet-4-5";
    private int maxBlocks = 1000000;
    private int blocksPerTick = 500;
    private int timeoutSeconds = 60;

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
}
