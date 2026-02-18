package craftassist.api;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void tokenBucket_initialState_canConsumeCapacityTimes() {
        RateLimiter.TokenBucket bucket = new RateLimiter.TokenBucket(3, Integer.MAX_VALUE);

        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
        assertTrue(bucket.tryConsume());
    }

    @Test
    void tokenBucket_exhausted_returnsFalse() {
        RateLimiter.TokenBucket bucket = new RateLimiter.TokenBucket(2, Integer.MAX_VALUE);

        bucket.tryConsume();
        bucket.tryConsume();
        assertFalse(bucket.tryConsume());
    }

    @Test
    void tokenBucket_capacityOne_exactlyOneConsume() {
        RateLimiter.TokenBucket bucket = new RateLimiter.TokenBucket(1, Integer.MAX_VALUE);

        assertTrue(bucket.tryConsume());
        assertFalse(bucket.tryConsume());
    }

    @Test
    void cleanup_removesPlayerBucket() {
        UUID uuid = UUID.randomUUID();

        // 消耗所有 token
        for (int i = 0; i < 3; i++) {
            RateLimiter.tryConsume(uuid);
        }
        // 此時被拒絕
        assertFalse(RateLimiter.tryConsume(uuid));

        // cleanup 後重新建立 bucket，應可再次消耗
        RateLimiter.cleanup(uuid);
        assertTrue(RateLimiter.tryConsume(uuid));
    }

    @Test
    void differentUuids_haveIndependentBuckets() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        // 耗盡 uuid1
        for (int i = 0; i < 3; i++) {
            RateLimiter.tryConsume(uuid1);
        }
        assertFalse(RateLimiter.tryConsume(uuid1));

        // uuid2 仍有 token
        assertTrue(RateLimiter.tryConsume(uuid2));

        // 清理
        RateLimiter.cleanup(uuid1);
        RateLimiter.cleanup(uuid2);
    }
}
