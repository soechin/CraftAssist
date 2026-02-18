package craftassist.api;

import craftassist.config.ConfigManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {

    private static final Map<UUID, TokenBucket> buckets = new ConcurrentHashMap<>();

    public static void init() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            cleanup(handler.getPlayer().getUUID());
        });
    }

    public static boolean tryConsume(UUID playerUuid) {
        TokenBucket bucket = buckets.computeIfAbsent(playerUuid, k -> {
            var config = ConfigManager.getConfig();
            return new TokenBucket(config.getRateLimitTokens(), config.getRateLimitRefillSeconds());
        });
        return bucket.tryConsume();
    }

    public static void cleanup(UUID playerUuid) {
        buckets.remove(playerUuid);
    }

    static class TokenBucket {
        private final int capacity;
        private final long refillIntervalNanos;
        private int tokens;
        private long lastRefillTime;

        TokenBucket(int capacity, int refillSeconds) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillIntervalNanos = refillSeconds * 1_000_000_000L;
            this.lastRefillTime = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            if (now - lastRefillTime >= refillIntervalNanos) {
                tokens = capacity;
                lastRefillTime = now;
            }
        }
    }
}
