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
package com.getboot.limiter.infrastructure.autoconfigure;

import com.getboot.limiter.api.registry.RateLimiterRegistry;
import com.getboot.limiter.spi.RateLimiterAlgorithmHandler;
import com.getboot.limiter.spi.RateLimiterRegistryCustomizer;
import com.getboot.limiter.support.aop.RateLimitAspect;
import com.getboot.limiter.support.resolver.RateLimitOperationResolver;
import com.getboot.limiter.support.registry.DefaultRateLimiterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 限流模块核心自动配置。
 *
 * <p>统一收敛算法处理器、注册表与切面，避免算法实现重复装配公共能力。</p>
 *
 * @author qiheng
 */
@AutoConfiguration
@ConditionalOnBean(RateLimiterAlgorithmHandler.class)
@ConditionalOnProperty(prefix = "getboot.limiter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimiterCoreAutoConfiguration {

    /**
     * 注册限流注册表。
     *
     * @param algorithmHandlers 算法处理器集合
     * @param registryCustomizers 注册表定制器集合
     * @return 限流注册表
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimiterRegistry rateLimiterRegistry(
            ObjectProvider<RateLimiterAlgorithmHandler> algorithmHandlers,
            ObjectProvider<RateLimiterRegistryCustomizer> registryCustomizers) {
        DefaultRateLimiterRegistry registry =
                new DefaultRateLimiterRegistry(algorithmHandlers.orderedStream().toList());
        registryCustomizers.orderedStream().forEach(customizer -> customizer.customize(registry));
        return registry;
    }

    /**
     * 注册限流操作解析器。
     *
     * @return 限流操作解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimitOperationResolver rateLimitOperationResolver() {
        return new RateLimitOperationResolver();
    }

    /**
     * 注册方法级限流切面。
     *
     * @param rateLimiterRegistry 限流注册表
     * @param rateLimitOperationResolver 限流操作解析器
     * @return 限流切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
    public RateLimitAspect rateLimitAspect(RateLimiterRegistry rateLimiterRegistry,
                                           RateLimitOperationResolver rateLimitOperationResolver) {
        return new RateLimitAspect(rateLimiterRegistry, rateLimitOperationResolver);
    }
}
