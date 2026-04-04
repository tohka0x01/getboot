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
package com.getboot.lock.infrastructure.redis.redisson.autoconfigure;

import com.getboot.lock.infrastructure.redis.redisson.aspect.DistributedLockAspect;
import com.getboot.lock.spi.DistributedLockAcquireFailureHandler;
import com.getboot.lock.spi.DistributedLockKeyResolver;
import com.getboot.lock.support.DefaultDistributedLockAcquireFailureHandler;
import com.getboot.lock.support.SpelDistributedLockKeyResolver;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Redis 分布式锁自动配置测试。
 *
 * @author qiheng
 */
class DistributedLockAutoConfigurationTest {

    /**
     * 测试用上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DistributedLockAutoConfiguration.class));

    /**
     * 验证存在 RedissonClient 且 Redis 锁开启时会注册默认锁 Bean。
     */
    @Test
    void shouldRegisterRedisLockBeansWhenRedissonPresent() {
        contextRunner
                .withBean(RedissonClient.class, this::createRedissonClient)
                .run(context -> {
                    assertInstanceOf(SpelDistributedLockKeyResolver.class, context.getBean(DistributedLockKeyResolver.class));
                    assertInstanceOf(
                            DefaultDistributedLockAcquireFailureHandler.class,
                            context.getBean(DistributedLockAcquireFailureHandler.class)
                    );
                    assertInstanceOf(DistributedLockAspect.class, context.getBean(DistributedLockAspect.class));
                });
    }

    /**
     * 验证缺少 RedissonClient 时不会注册 Redis 锁相关 Bean。
     */
    @Test
    void shouldSkipRedisLockBeansWhenRedissonMissing() {
        contextRunner.run(context -> {
            assertFalse(context.containsBean("distributedLockKeyResolver"));
            assertFalse(context.containsBean("distributedLockAcquireFailureHandler"));
            assertFalse(context.containsBean("distributedLockAspect"));
        });
    }

    /**
     * 验证关闭 Redis 锁实现时不会注册 Redis 锁切面。
     */
    @Test
    void shouldSkipRedisLockBeansWhenRedisLockDisabled() {
        contextRunner
                .withPropertyValues("getboot.lock.redis.enabled=false")
                .withBean(RedissonClient.class, this::createRedissonClient)
                .run(context -> {
                    assertFalse(context.containsBean("distributedLockKeyResolver"));
                    assertFalse(context.containsBean("distributedLockAcquireFailureHandler"));
                    assertFalse(context.containsBean("distributedLockAspect"));
                });
    }

    /**
     * 构造测试用 RedissonClient 代理。
     *
     * @return RedissonClient 代理
     */
    private RedissonClient createRedissonClient() {
        return (RedissonClient) Proxy.newProxyInstance(
                RedissonClient.class.getClassLoader(),
                new Class[]{RedissonClient.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> "TestRedissonClient";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    /**
     * 返回常见返回类型的默认值。
     *
     * @param returnType 返回类型
     * @return 默认值
     */
    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class == returnType) {
            return false;
        }
        if (byte.class == returnType) {
            return (byte) 0;
        }
        if (short.class == returnType) {
            return (short) 0;
        }
        if (int.class == returnType) {
            return 0;
        }
        if (long.class == returnType) {
            return 0L;
        }
        if (float.class == returnType) {
            return 0F;
        }
        if (double.class == returnType) {
            return 0D;
        }
        if (char.class == returnType) {
            return '\0';
        }
        return null;
    }
}
