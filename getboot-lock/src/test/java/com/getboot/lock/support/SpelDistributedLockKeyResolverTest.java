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
package com.getboot.lock.support;

import com.getboot.lock.api.annotation.DistributedLock;
import com.getboot.lock.api.exception.DistributedLockException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link SpelDistributedLockKeyResolver} 测试。
 *
 * @author qiheng
 */
class SpelDistributedLockKeyResolverTest {

    /**
     * 验证显式声明的固定 key 优先于 SpEL 表达式。
     *
     * @throws NoSuchMethodException 方法不存在时抛出
     */
    @Test
    void shouldPreferFixedKeyOverKeyExpression() throws NoSuchMethodException {
        Method method = DemoService.class.getDeclaredMethod("fixedKey", String.class);
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        SpelDistributedLockKeyResolver resolver = new SpelDistributedLockKeyResolver();

        String key = resolver.resolve(joinPointWithArgs("order-1"), method, distributedLock);

        assertEquals("fixed-order", key);
    }

    /**
     * 验证可以基于方法参数解析 SpEL 锁键。
     *
     * @throws NoSuchMethodException 方法不存在时抛出
     */
    @Test
    void shouldResolveKeyFromMethodArguments() throws NoSuchMethodException {
        Method method = DemoService.class.getDeclaredMethod("spelKey", String.class);
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        SpelDistributedLockKeyResolver resolver = new SpelDistributedLockKeyResolver();

        String key = resolver.resolve(joinPointWithArgs("order-2"), method, distributedLock);

        assertEquals("order-2", key);
    }

    /**
     * 验证既没有固定 key 也没有表达式时会直接报错。
     *
     * @throws NoSuchMethodException 方法不存在时抛出
     */
    @Test
    void shouldThrowWhenBothKeyAndExpressionMissing() throws NoSuchMethodException {
        Method method = DemoService.class.getDeclaredMethod("missingKey", String.class);
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        SpelDistributedLockKeyResolver resolver = new SpelDistributedLockKeyResolver();

        DistributedLockException exception = assertThrows(
                DistributedLockException.class,
                () -> resolver.resolve(joinPointWithArgs("order-3"), method, distributedLock)
        );

        assertEquals("Distributed lock key must not be empty.", exception.getMessage());
    }

    /**
     * 创建仅实现参数访问能力的切点代理。
     *
     * @param args 方法参数
     * @return 切点代理
     */
    private ProceedingJoinPoint joinPointWithArgs(Object... args) {
        return (ProceedingJoinPoint) Proxy.newProxyInstance(
                ProceedingJoinPoint.class.getClassLoader(),
                new Class[]{ProceedingJoinPoint.class},
                (proxy, method, methodArgs) -> switch (method.getName()) {
                    case "getArgs" -> args;
                    case "toString" -> "TestProceedingJoinPoint";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == methodArgs[0];
                    default -> null;
                }
        );
    }

    /**
     * 测试用服务定义。
     */
    static class DemoService {

        /**
         * 固定 key 示例。
         *
         * @param orderNo 订单号
         */
        @DistributedLock(scene = "order", key = "fixed-order", keyExpression = "#orderNo")
        void fixedKey(String orderNo) {
        }

        /**
         * SpEL key 示例。
         *
         * @param orderNo 订单号
         */
        @DistributedLock(scene = "order", keyExpression = "#orderNo")
        void spelKey(String orderNo) {
        }

        /**
         * 缺少 key 示例。
         *
         * @param orderNo 订单号
         */
        @DistributedLock(scene = "order")
        void missingKey(String orderNo) {
        }
    }
}
