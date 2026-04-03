/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.getboot.limiter.infrastructure.leakybucket.redisson.support;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Redis 漏桶限流底层支持。
 *
 * <p>使用 Redis 字符串保存桶状态，并通过分布式锁保证并发更新的一致性。</p>
 *
 * @author qiheng
 */
public class LeakyBucketRedisSupport {

    /**
     * 漏桶状态 key 后缀。
     */
    private static final String STATE_KEY_SUFFIX = ":state";

    /**
     * 漏桶锁 key 后缀。
     */
    private static final String LOCK_KEY_SUFFIX = ":lock";

    /**
     * 最小状态存活时间，单位毫秒。
     */
    private static final long MIN_TTL_MILLIS = 1000L;

    /**
     * Redisson 客户端。
     */
    private final RedissonClient redissonClient;

    /**
     * Redis key 前缀。
     */
    private final String keyPrefix;

    /**
     * 当前时间提供器。
     */
    private final LongSupplier currentTimeSupplier;

    /**
     * 使用系统时间创建漏桶 Redis 支撑组件。
     *
     * @param redissonClient Redisson 客户端
     * @param keyPrefix Redis key 前缀
     */
    public LeakyBucketRedisSupport(RedissonClient redissonClient, String keyPrefix) {
        this(redissonClient, keyPrefix, System::currentTimeMillis);
    }

    /**
     * 创建可注入时间源的漏桶 Redis 支撑组件。
     *
     * @param redissonClient Redisson 客户端
     * @param keyPrefix Redis key 前缀
     * @param currentTimeSupplier 当前时间提供器
     */
    LeakyBucketRedisSupport(RedissonClient redissonClient, String keyPrefix, LongSupplier currentTimeSupplier) {
        this.redissonClient = redissonClient;
        this.keyPrefix = keyPrefix;
        this.currentTimeSupplier = currentTimeSupplier;
    }

    /**
     * 尝试在漏桶中加入指定数量请求。
     *
     * @param limiterName 限流器名称
     * @param capacity 漏桶容量
     * @param interval 时间窗口大小
     * @param intervalUnit 时间窗口单位
     * @param permits 许可数量
     * @return 是否获取成功
     */
    public boolean tryAcquire(String limiterName, long capacity, long interval, TimeUnit intervalUnit, long permits) {
        long intervalMillis = intervalUnit.toMillis(interval);
        if (intervalMillis <= 0L) {
            throw new IllegalArgumentException("Leaky bucket interval must be at least 1 millisecond.");
        }
        long now = currentTimeSupplier.getAsLong();
        RLock lock = redissonClient.getLock(buildLockKey(limiterName));
        boolean locked = lock.tryLock();
        if (!locked) {
            return false;
        }
        try {
            RBucket<String> stateBucket = redissonClient.getBucket(buildStateKey(limiterName), StringCodec.INSTANCE);
            BucketState currentState = BucketState.deserialize(stateBucket.get(), now);
            BucketState leakedState = currentState.leak(now, capacity, intervalMillis);
            if (leakedState.waterLevel + permits > capacity) {
                persistState(stateBucket, leakedState, intervalMillis);
                return false;
            }
            BucketState updatedState = leakedState.add(now, permits);
            persistState(stateBucket, updatedState, intervalMillis);
            return true;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 删除漏桶底层状态。
     *
     * @param limiterName 限流器名称
     * @return 是否删除成功
     */
    public boolean delete(String limiterName) {
        long deleted = redissonClient.getKeys().delete(
                buildStateKey(limiterName),
                buildLockKey(limiterName)
        );
        return deleted > 0;
    }

    /**
     * 持久化漏桶状态。
     *
     * @param stateBucket 状态桶
     * @param state 漏桶状态
     * @param intervalMillis 窗口毫秒数
     */
    private void persistState(RBucket<String> stateBucket, BucketState state, long intervalMillis) {
        stateBucket.set(state.serialize(), ttlMillis(intervalMillis), TimeUnit.MILLISECONDS);
    }

    /**
     * 计算状态 TTL。
     *
     * @param intervalMillis 窗口毫秒数
     * @return TTL 毫秒数
     */
    private long ttlMillis(long intervalMillis) {
        if (intervalMillis > Long.MAX_VALUE / 2) {
            return Long.MAX_VALUE;
        }
        return Math.max(intervalMillis * 2, MIN_TTL_MILLIS);
    }

    /**
     * 构建漏桶状态 key。
     *
     * @param limiterName 限流器名称
     * @return 状态 key
     */
    private String buildStateKey(String limiterName) {
        return keyPrefix + ":" + limiterName + STATE_KEY_SUFFIX;
    }

    /**
     * 构建漏桶锁 key。
     *
     * @param limiterName 限流器名称
     * @return 锁 key
     */
    private String buildLockKey(String limiterName) {
        return keyPrefix + ":" + limiterName + LOCK_KEY_SUFFIX;
    }

    /**
     * 漏桶状态。
     *
     * @param lastLeakTimestamp 最近一次漏水时间戳
     * @param waterLevel 当前水位
     */
    private record BucketState(long lastLeakTimestamp, long waterLevel) {

        /**
         * 反序列化漏桶状态。
         *
         * @param rawState 原始状态串
         * @param now 当前时间
         * @return 漏桶状态
         */
        private static BucketState deserialize(String rawState, long now) {
            if (rawState == null || rawState.trim().isEmpty()) {
                return new BucketState(now, 0L);
            }
            String[] parts = rawState.split(":");
            if (parts.length != 2) {
                throw new IllegalStateException("Malformed leaky bucket state: " + rawState);
            }
            try {
                long lastLeakTimestamp = Long.parseLong(parts[0]);
                long waterLevel = Long.parseLong(parts[1]);
                return new BucketState(lastLeakTimestamp, Math.max(0L, waterLevel));
            } catch (NumberFormatException ex) {
                throw new IllegalStateException("Malformed leaky bucket state: " + rawState, ex);
            }
        }

        /**
         * 根据时间推进漏桶出水。
         *
         * @param now 当前时间
         * @param capacity 漏桶容量
         * @param intervalMillis 窗口毫秒数
         * @return 漏水后的状态
         */
        private BucketState leak(long now, long capacity, long intervalMillis) {
            if (waterLevel <= 0L || now <= lastLeakTimestamp) {
                return waterLevel <= 0L ? new BucketState(now, 0L) : this;
            }
            long leakedPermits = (long) Math.floor((double) (now - lastLeakTimestamp) * (double) capacity / intervalMillis);
            if (leakedPermits <= 0L) {
                return this;
            }
            long newWaterLevel = Math.max(0L, waterLevel - leakedPermits);
            if (newWaterLevel == 0L) {
                return new BucketState(now, 0L);
            }
            long advancedMillis = (long) Math.floor((double) leakedPermits * intervalMillis / capacity);
            long nextLastLeakTimestamp = advancedMillis > 0L
                    ? Math.min(now, lastLeakTimestamp + advancedMillis)
                    : now;
            return new BucketState(nextLastLeakTimestamp, newWaterLevel);
        }

        /**
         * 向漏桶中增加水位。
         *
         * @param now 当前时间
         * @param permits 新增许可数
         * @return 更新后的状态
         */
        private BucketState add(long now, long permits) {
            long nextLastLeakTimestamp = waterLevel == 0L ? now : lastLeakTimestamp;
            return new BucketState(nextLastLeakTimestamp, waterLevel + permits);
        }

        /**
         * 序列化漏桶状态。
         *
         * @return 序列化字符串
         */
        private String serialize() {
            return lastLeakTimestamp + ":" + waterLevel;
        }
    }
}
