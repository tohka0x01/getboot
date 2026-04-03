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
package com.getboot.limiter.infrastructure.slidingwindow.redisson.support;

import org.redisson.api.RLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 滑动窗口限流底层支持。
 *
 * <p>使用有序集合记录时间窗口内的请求事件，并通过分布式锁保证并发判断与写入的一致性。</p>
 *
 * @author qiheng
 */
public class SlidingWindowRedisSupport {

    /**
     * 滑动窗口数据 key 后缀。
     */
    private static final String WINDOW_KEY_SUFFIX = ":window";

    /**
     * 滑动窗口锁 key 后缀。
     */
    private static final String LOCK_KEY_SUFFIX = ":lock";

    /**
     * Redisson 客户端。
     */
    private final RedissonClient redissonClient;

    /**
     * Redis key 前缀。
     */
    private final String keyPrefix;

    /**
     * 创建滑动窗口 Redis 支撑组件。
     *
     * @param redissonClient Redisson 客户端
     * @param keyPrefix Redis key 前缀
     */
    public SlidingWindowRedisSupport(RedissonClient redissonClient, String keyPrefix) {
        this.redissonClient = redissonClient;
        this.keyPrefix = keyPrefix;
    }

    /**
     * 尝试在滑动窗口中写入请求事件。
     *
     * @param limiterName 限流器名称
     * @param maxRequests 最大请求数
     * @param interval 时间窗口大小
     * @param intervalUnit 时间窗口单位
     * @param permits 许可数量
     * @return 是否获取成功
     */
    public boolean tryAcquire(String limiterName, long maxRequests, long interval, TimeUnit intervalUnit, long permits) {
        long now = System.currentTimeMillis();
        long windowMillis = intervalUnit.toMillis(interval);
        String redisKey = buildWindowKey(limiterName);
        RLock lock = redissonClient.getLock(buildLockKey(limiterName));
        boolean locked = lock.tryLock();
        if (!locked) {
            return false;
        }
        try {
            RScoredSortedSet<String> window = redissonClient.getScoredSortedSet(redisKey);
            purgeExpired(window, now, windowMillis);
            long currentRequests = window.size();
            if (currentRequests + permits > maxRequests) {
                return false;
            }
            for (long i = 0; i < permits; i++) {
                window.add(now, now + "-" + i + "-" + UUID.randomUUID());
            }
            window.expire(Duration.ofMillis(Math.max(windowMillis * 2, 1000L)));
            return true;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取当前窗口内的请求数。
     *
     * @param limiterName 限流器名称
     * @param interval 时间窗口大小
     * @param intervalUnit 时间窗口单位
     * @return 当前请求数
     */
    public long currentRequests(String limiterName, long interval, TimeUnit intervalUnit) {
        long now = System.currentTimeMillis();
        long windowMillis = intervalUnit.toMillis(interval);
        RScoredSortedSet<String> window = redissonClient.getScoredSortedSet(buildWindowKey(limiterName));
        purgeExpired(window, now, windowMillis);
        return window.size();
    }

    /**
     * 删除滑动窗口底层状态。
     *
     * @param limiterName 限流器名称
     * @return 是否删除成功
     */
    public boolean delete(String limiterName) {
        long deleted = redissonClient.getKeys().delete(
                buildWindowKey(limiterName),
                buildLockKey(limiterName)
        );
        return deleted > 0;
    }

    /**
     * 清理窗口中已过期的请求事件。
     *
     * @param window 滑动窗口集合
     * @param now 当前时间
     * @param windowMillis 窗口毫秒数
     */
    private void purgeExpired(RScoredSortedSet<String> window, long now, long windowMillis) {
        long threshold = now - windowMillis;
        window.removeRangeByScore(0, true, threshold, true);
    }

    /**
     * 构建滑动窗口数据 key。
     *
     * @param limiterName 限流器名称
     * @return 数据 key
     */
    private String buildWindowKey(String limiterName) {
        return keyPrefix + ":" + limiterName + WINDOW_KEY_SUFFIX;
    }

    /**
     * 构建滑动窗口锁 key。
     *
     * @param limiterName 限流器名称
     * @return 锁 key
     */
    private String buildLockKey(String limiterName) {
        return keyPrefix + ":" + limiterName + LOCK_KEY_SUFFIX;
    }
}
