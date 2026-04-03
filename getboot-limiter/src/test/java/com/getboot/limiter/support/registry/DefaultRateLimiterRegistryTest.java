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
package com.getboot.limiter.support.registry;

import com.getboot.limiter.api.model.LimiterAlgorithm;
import com.getboot.limiter.api.model.LimiterRule;
import com.getboot.limiter.spi.RateLimiterAlgorithmHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 默认限流注册表测试。
 */
class DefaultRateLimiterRegistryTest {

    /**
     * 验证预定义限流器会路由到对应算法处理器。
     */
    @Test
    void shouldRoutePredefinedLimiterToMatchingAlgorithm() {
        FakeHandler slidingWindowHandler = new FakeHandler(LimiterAlgorithm.SLIDING_WINDOW, Map.of());
        FakeHandler tokenBucketHandler = new FakeHandler(
                LimiterAlgorithm.TOKEN_BUCKET,
                Map.of("sms", rule(LimiterAlgorithm.TOKEN_BUCKET, 5, 1, "SECONDS"))
        );
        DefaultRateLimiterRegistry registry =
                new DefaultRateLimiterRegistry(List.of(slidingWindowHandler, tokenBucketHandler));

        assertTrue(registry.tryAcquire("sms"));
        assertEquals(0, slidingWindowHandler.tryAcquireCalls);
        assertEquals(1, tokenBucketHandler.tryAcquireCalls);
        assertEquals("sms", tokenBucketHandler.lastLimiterName);
        assertEquals(LimiterAlgorithm.TOKEN_BUCKET, tokenBucketHandler.lastRule.getAlgorithm());
    }

    /**
     * 验证配置规则时缺省算法会回退到滑动窗口。
     */
    @Test
    void shouldDefaultMissingAlgorithmToSlidingWindowWhenConfiguringRule() {
        FakeHandler slidingWindowHandler = new FakeHandler(LimiterAlgorithm.SLIDING_WINDOW, Map.of());
        FakeHandler tokenBucketHandler = new FakeHandler(LimiterAlgorithm.TOKEN_BUCKET, Map.of());
        DefaultRateLimiterRegistry registry =
                new DefaultRateLimiterRegistry(List.of(slidingWindowHandler, tokenBucketHandler));
        LimiterRule limiterRule = new LimiterRule();
        limiterRule.setRate(12);
        limiterRule.setInterval(2);
        limiterRule.setIntervalUnit("SECONDS");

        registry.configureRateLimiter("login", limiterRule);

        assertTrue(registry.tryAcquire("login", 2));
        assertEquals(1, slidingWindowHandler.tryAcquireCalls);
        assertEquals(2, slidingWindowHandler.lastPermits);
        assertEquals(LimiterAlgorithm.SLIDING_WINDOW, slidingWindowHandler.lastRule.getAlgorithm());
    }

    /**
     * 验证预定义限流器名称跨算法重复时会快速失败。
     */
    @Test
    void shouldFailFastWhenPredefinedLimiterNameDuplicatedAcrossAlgorithms() {
        FakeHandler slidingWindowHandler = new FakeHandler(
                LimiterAlgorithm.SLIDING_WINDOW,
                Map.of("duplicate", rule(LimiterAlgorithm.SLIDING_WINDOW, 10, 1, "SECONDS"))
        );
        FakeHandler tokenBucketHandler = new FakeHandler(
                LimiterAlgorithm.TOKEN_BUCKET,
                Map.of("duplicate", rule(LimiterAlgorithm.TOKEN_BUCKET, 5, 1, "SECONDS"))
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new DefaultRateLimiterRegistry(List.of(slidingWindowHandler, tokenBucketHandler)));

        assertTrue(exception.getMessage().contains("duplicate"));
    }

    /**
     * 验证运行时规则在未预配置时也能正确路由。
     */
    @Test
    void shouldRouteRuntimeRuleWithoutPreconfiguration() {
        FakeHandler slidingWindowHandler = new FakeHandler(LimiterAlgorithm.SLIDING_WINDOW, Map.of());
        FakeHandler tokenBucketHandler = new FakeHandler(LimiterAlgorithm.TOKEN_BUCKET, Map.of());
        DefaultRateLimiterRegistry registry =
                new DefaultRateLimiterRegistry(List.of(slidingWindowHandler, tokenBucketHandler));

        assertTrue(registry.tryAcquire(
                "send-sms:13800000000",
                rule(LimiterAlgorithm.TOKEN_BUCKET, 1, 60, "SECONDS"),
                1,
                0,
                TimeUnit.SECONDS
        ));
        assertEquals(0, slidingWindowHandler.tryAcquireCalls);
        assertEquals(1, tokenBucketHandler.tryAcquireCalls);
        assertEquals("send-sms:13800000000", tokenBucketHandler.lastLimiterName);
        assertEquals(LimiterAlgorithm.TOKEN_BUCKET, tokenBucketHandler.lastRule.getAlgorithm());
    }

    /**
     * 创建测试用限流规则。
     *
     * @param algorithm 限流算法
     * @param rate 速率阈值
     * @param interval 时间窗口大小
     * @param intervalUnit 时间窗口单位
     * @return 限流规则
     */
    private static LimiterRule rule(LimiterAlgorithm algorithm, long rate, long interval, String intervalUnit) {
        LimiterRule limiterRule = new LimiterRule();
        limiterRule.setAlgorithm(algorithm);
        limiterRule.setRate(rate);
        limiterRule.setInterval(interval);
        limiterRule.setIntervalUnit(intervalUnit);
        return limiterRule;
    }

    /**
     * 测试用算法处理器。
     */
    private static final class FakeHandler implements RateLimiterAlgorithmHandler {

        /**
         * 处理器算法类型。
         */
        private final LimiterAlgorithm algorithm;

        /**
         * 预定义规则集合。
         */
        private final Map<String, LimiterRule> predefinedRules;

        /**
         * 默认规则。
         */
        private final LimiterRule defaultRule;

        /**
         * tryAcquire 调用次数。
         */
        private int tryAcquireCalls;

        /**
         * 最近一次限流器名称。
         */
        private String lastLimiterName;

        /**
         * 最近一次规则快照。
         */
        private LimiterRule lastRule;

        /**
         * 最近一次许可数量。
         */
        private long lastPermits;

        /**
         * 创建测试用算法处理器。
         *
         * @param algorithm 算法类型
         * @param predefinedRules 预定义规则
         */
        private FakeHandler(LimiterAlgorithm algorithm, Map<String, LimiterRule> predefinedRules) {
            this.algorithm = algorithm;
            this.predefinedRules = predefinedRules;
            this.defaultRule = rule(algorithm, 10, 1, "SECONDS");
        }

        /**
         * 返回处理器算法类型。
         *
         * @return 算法类型
         */
        @Override
        public LimiterAlgorithm algorithm() {
            return algorithm;
        }

        /**
         * 返回预定义规则集合。
         *
         * @return 预定义规则集合
         */
        @Override
        public Map<String, LimiterRule> predefinedRules() {
            return predefinedRules;
        }

        /**
         * 返回默认规则副本。
         *
         * @return 默认规则
         */
        @Override
        public LimiterRule defaultRule() {
            return defaultRule.copy();
        }

        /**
         * 返回默认等待时长。
         *
         * @return 默认等待秒数
         */
        @Override
        public long defaultTimeout() {
            return 1;
        }

        /**
         * 校验规则是否与处理器算法一致。
         *
         * @param rule 限流规则
         */
        @Override
        public void validateRule(LimiterRule rule) {
            if (rule.getAlgorithm() != algorithm) {
                throw new IllegalArgumentException("Unexpected algorithm: " + rule.getAlgorithm());
            }
            if (rule.getRate() <= 0 || rule.getInterval() <= 0) {
                throw new IllegalArgumentException("Rate and interval must be greater than 0.");
            }
        }

        /**
         * 记录一次许可申请。
         *
         * @param limiterName 限流器名称
         * @param rule 限流规则
         * @param permits 许可数量
         * @return 始终返回成功
         */
        @Override
        public boolean tryAcquire(String limiterName, LimiterRule rule, long permits) {
            this.tryAcquireCalls++;
            this.lastLimiterName = limiterName;
            this.lastRule = rule.copy();
            this.lastPermits = permits;
            return true;
        }

        /**
         * 删除限流器状态。
         *
         * @param limiterName 限流器名称
         * @return 始终返回成功
         */
        @Override
        public boolean delete(String limiterName) {
            return true;
        }
    }
}
