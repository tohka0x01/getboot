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

import com.getboot.limiter.api.limiter.RateLimiter;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis / Redisson 的滑动窗口限流器。
 *
 * @author qiheng
 */
public class RedissonSlidingWindowRateLimiter implements RateLimiter {

    /**
     * 滑动窗口 Redis 支撑组件。
     */
    private final SlidingWindowRedisSupport slidingWindowRedisSupport;

    /**
     * 创建滑动窗口限流器适配器。
     *
     * @param slidingWindowRedisSupport Redis 支撑组件
     */
    public RedissonSlidingWindowRateLimiter(SlidingWindowRedisSupport slidingWindowRedisSupport) {
        this.slidingWindowRedisSupport = slidingWindowRedisSupport;
    }

    /**
     * 以秒级窗口执行一次限流校验。
     *
     * @param key 业务 key
     * @param limit 限流阈值
     * @param windowSize 窗口大小
     * @return 是否允许通过
     */
    @Override
    public boolean tryAcquire(String key, int limit, int windowSize) {
        return slidingWindowRedisSupport.tryAcquire(key, limit, windowSize, TimeUnit.SECONDS, 1);
    }
}
