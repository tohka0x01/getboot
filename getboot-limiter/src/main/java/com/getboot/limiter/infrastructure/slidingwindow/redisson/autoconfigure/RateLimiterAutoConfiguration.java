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
package com.getboot.limiter.infrastructure.slidingwindow.redisson.autoconfigure;

import com.getboot.limiter.api.limiter.RateLimiter;
import com.getboot.limiter.api.properties.SlidingWindowRateLimiterProperties;
import com.getboot.limiter.infrastructure.slidingwindow.redisson.support.RedissonSlidingWindowRateLimiter;
import com.getboot.limiter.infrastructure.slidingwindow.redisson.support.RedissonSlidingWindowRateLimiterHandler;
import com.getboot.limiter.infrastructure.slidingwindow.redisson.support.SlidingWindowRedisSupport;
import com.getboot.limiter.spi.RateLimiterAlgorithmHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 滑动窗口限流自动配置。
 *
 * <p>当前实现基于 Redis / Redisson，后续可在 limiter 模块中继续补充令牌桶、漏桶等其他算法。</p>
 *
 * @author qiheng
 */
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnBean(RedissonClient.class)
@ConditionalOnProperty(prefix = "getboot.limiter", name = {"enabled", "sliding-window.enabled"}, havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SlidingWindowRateLimiterProperties.class)
public class RateLimiterAutoConfiguration {

    /**
     * 注册滑动窗口 Redis 支撑组件。
     *
     * @param redissonClient Redisson 客户端
     * @param properties 滑动窗口配置
     * @return Redis 支撑组件
     */
    @Bean
    @ConditionalOnMissingBean
    public SlidingWindowRedisSupport slidingWindowRedisSupport(RedissonClient redissonClient,
                                                               SlidingWindowRateLimiterProperties properties) {
        return new SlidingWindowRedisSupport(redissonClient, properties.getKeyPrefix());
    }

    /**
     * 注册滑动窗口算法处理器。
     *
     * @param slidingWindowRedisSupport Redis 支撑组件
     * @param properties 滑动窗口配置
     * @return 算法处理器
     */
    @Bean
    @ConditionalOnMissingBean(name = "redissonSlidingWindowRateLimiterHandler")
    public RateLimiterAlgorithmHandler redissonSlidingWindowRateLimiterHandler(
            SlidingWindowRedisSupport slidingWindowRedisSupport,
            SlidingWindowRateLimiterProperties properties) {
        return new RedissonSlidingWindowRateLimiterHandler(slidingWindowRedisSupport, properties);
    }

    /**
     * 注册滑动窗口限流器适配器。
     *
     * @param slidingWindowRedisSupport Redis 支撑组件
     * @return 限流器适配器
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimiter redissonSlidingWindowRateLimiter(SlidingWindowRedisSupport slidingWindowRedisSupport) {
        return new RedissonSlidingWindowRateLimiter(slidingWindowRedisSupport);
    }
}
