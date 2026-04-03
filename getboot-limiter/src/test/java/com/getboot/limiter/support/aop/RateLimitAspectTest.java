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
package com.getboot.limiter.support.aop;

import com.getboot.limiter.api.annotation.RateLimit;
import com.getboot.limiter.api.model.LimiterAlgorithm;
import com.getboot.limiter.api.model.LimiterRule;
import com.getboot.limiter.api.registry.RateLimiterRegistry;
import com.getboot.limiter.support.resolver.RateLimitOperationResolver;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 方法级限流切面测试。
 */
class RateLimitAspectTest {

    /**
     * 验证 JDK 代理场景下能够解析到实现类方法。
     */
    @Test
    void shouldResolveImplementationMethodForJdkProxy() {
        CapturingRateLimiterRegistry registry = new CapturingRateLimiterRegistry();
        RateLimitAspect aspect = new RateLimitAspect(registry, new RateLimitOperationResolver());
        SmsService target = new SmsServiceImpl();

        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.setInterfaces(SmsService.class);
        proxyFactory.addAspect(aspect);

        SmsService proxy = proxyFactory.getProxy();
        proxy.send("13800000000");

        assertEquals("send-sms:13800000000", registry.limiterName);
        assertEquals(LimiterAlgorithm.TOKEN_BUCKET, registry.rule.getAlgorithm());
        assertEquals(1, registry.permits);
        assertEquals(0, registry.timeout);
        assertEquals(TimeUnit.SECONDS, registry.timeUnit);
    }

    /**
     * 测试用短信服务接口。
     */
    interface SmsService {

        /**
         * 发送短信。
         *
         * @param phone 手机号
         */
        void send(String phone);
    }

    /**
     * 测试用短信服务实现。
     */
    static final class SmsServiceImpl implements SmsService {

        /**
         * 发送短信。
         *
         * @param phone 手机号
         */
        @Override
        @RateLimit(
                scene = "send-sms",
                keyExpression = "#phone",
                algorithm = LimiterAlgorithm.TOKEN_BUCKET,
                rate = 1,
                interval = 60,
                intervalUnit = TimeUnit.SECONDS
        )
        public void send(String phone) {
        }
    }

    /**
     * 用于捕获切面入参的限流注册表。
     */
    static final class CapturingRateLimiterRegistry implements RateLimiterRegistry {

        /**
         * 捕获到的限流器名称。
         */
        private String limiterName;

        /**
         * 捕获到的限流规则。
         */
        private LimiterRule rule;

        /**
         * 捕获到的许可数量。
         */
        private long permits;

        /**
         * 捕获到的超时时长。
         */
        private long timeout;

        /**
         * 捕获到的超时单位。
         */
        private TimeUnit timeUnit;

        /**
         * 记录切面提交的限流参数。
         *
         * @param limiterName 限流器名称
         * @param rule 限流规则
         * @param permits 许可数量
         * @param timeout 超时时长
         * @param timeUnit 超时单位
         * @return 始终返回成功
         */
        @Override
        public boolean tryAcquire(String limiterName, LimiterRule rule, long permits, long timeout, TimeUnit timeUnit) {
            this.limiterName = limiterName;
            this.rule = rule;
            this.permits = permits;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            return true;
        }

        /**
         * 测试中不支持直接配置限流器。
         *
         * @param limiterName 限流器名称
         * @param config 限流配置
         */
        @Override
        public void configureRateLimiter(String limiterName, LimiterRule config) {
            throw new UnsupportedOperationException();
        }

        /**
         * 测试中不支持该重载。
         *
         * @param limiterName 限流器名称
         * @return 不返回
         */
        @Override
        public boolean tryAcquire(String limiterName) {
            throw new UnsupportedOperationException();
        }

        /**
         * 测试中不支持该重载。
         *
         * @param limiterName 限流器名称
         * @param permits 许可数量
         * @return 不返回
         */
        @Override
        public boolean tryAcquire(String limiterName, long permits) {
            throw new UnsupportedOperationException();
        }

        /**
         * 测试中不支持该重载。
         *
         * @param limiterName 限流器名称
         * @param timeout 超时时长
         * @param timeUnit 超时单位
         * @return 不返回
         */
        @Override
        public boolean tryAcquire(String limiterName, long timeout, TimeUnit timeUnit) {
            throw new UnsupportedOperationException();
        }

        /**
         * 测试中不支持该重载。
         *
         * @param limiterName 限流器名称
         * @param permits 许可数量
         * @param timeout 超时时长
         * @param timeUnit 超时单位
         * @return 不返回
         */
        @Override
        public boolean tryAcquire(String limiterName, long permits, long timeout, TimeUnit timeUnit) {
            throw new UnsupportedOperationException();
        }

        /**
         * 测试中不支持直接更新限流器配置。
         *
         * @param limiterName 限流器名称
         * @param newConfig 新配置
         */
        @Override
        public void updateRateLimiterConfig(String limiterName, LimiterRule newConfig) {
            throw new UnsupportedOperationException();
        }

        /**
         * 测试中不支持删除限流器。
         *
         * @param limiterName 限流器名称
         * @return 不返回
         */
        @Override
        public boolean deleteRateLimiter(String limiterName) {
            throw new UnsupportedOperationException();
        }
    }
}
