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
package com.getboot.idempotency.support;

import com.getboot.idempotency.api.annotation.Idempotent;
import com.getboot.idempotency.api.exception.IdempotencyException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SpEL 幂等 key 解析器测试。
 */
class SpelIdempotencyKeyResolverTest {

    /**
     * 被测解析器。
     */
    private final SpelIdempotencyKeyResolver resolver = new SpelIdempotencyKeyResolver();

    /**
     * 验证可根据方法参数解析 SpEL key。
     *
     * @throws Exception 反射异常
     */
    @Test
    void shouldResolveSpelKeyFromMethodArguments() throws Exception {
        Method method = SampleService.class.getMethod("process", String.class, OrderRequest.class);
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        String key = resolver.resolve(
                new StaticProceedingJoinPoint("order-1", new OrderRequest("user-1")),
                method,
                idempotent
        );

        assertEquals("order-1:user-1", key);
    }

    /**
     * 验证固定 key 的优先级高于表达式。
     *
     * @throws Exception 反射异常
     */
    @Test
    void shouldPreferFixedKeyOverExpression() throws Exception {
        Method method = SampleService.class.getMethod("processWithFixedKey", String.class);
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        String key = resolver.resolve(new StaticProceedingJoinPoint("order-2"), method, idempotent);

        assertEquals("fixed-key", key);
    }

    /**
     * 验证解析结果为空时会抛出异常。
     *
     * @throws Exception 反射异常
     */
    @Test
    void shouldRejectNullResolvedKey() throws Exception {
        Method method = SampleService.class.getMethod("processWithNullKey", String.class);
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        assertThrows(
                IdempotencyException.class,
                () -> resolver.resolve(new StaticProceedingJoinPoint("order-3"), method, idempotent)
        );
    }

    /**
     * 测试用业务服务。
     */
    static class SampleService {

        /**
         * 处理订单请求。
         *
         * @param orderNo 订单号
         * @param request 订单请求
         */
        @Idempotent(keyExpression = "#orderNo + ':' + #request.userId")
        public void process(String orderNo, OrderRequest request) {
        }

        /**
         * 使用固定 key 处理请求。
         *
         * @param orderNo 订单号
         */
        @Idempotent(key = "fixed-key", keyExpression = "#orderNo")
        public void processWithFixedKey(String orderNo) {
        }

        /**
         * 处理会产生空 key 的请求。
         *
         * @param orderNo 订单号
         */
        @Idempotent(keyExpression = "#missing")
        public void processWithNullKey(String orderNo) {
        }
    }

    /**
     * 测试用订单请求对象。
     */
    static class OrderRequest {

        /**
         * 用户标识。
         */
        private final String userId;

        /**
         * 创建订单请求。
         *
         * @param userId 用户标识
         */
        OrderRequest(String userId) {
            this.userId = userId;
        }

        /**
         * 返回用户标识。
         *
         * @return 用户标识
         */
        public String getUserId() {
            return userId;
        }
    }

    /**
     * 固定入参的切点实现。
     */
    static class StaticProceedingJoinPoint implements ProceedingJoinPoint {

        /**
         * 方法参数数组。
         */
        private final Object[] args;

        /**
         * 创建固定入参的切点对象。
         *
         * @param args 方法参数
         */
        StaticProceedingJoinPoint(Object... args) {
            this.args = args;
        }

        /**
         * 测试中不支持直接继续执行。
         *
         * @return 不返回
         */
        @Override
        public Object proceed() {
            throw new UnsupportedOperationException();
        }

        /**
         * 测试中不支持替换参数继续执行。
         *
         * @param args 新参数
         * @return 不返回
         */
        @Override
        public Object proceed(Object[] args) {
            throw new UnsupportedOperationException();
        }

        /**
         * 返回当前代理对象。
         *
         * @return 始终为 null
         */
        @Override
        public Object getThis() {
            return null;
        }

        /**
         * 返回目标对象。
         *
         * @return 始终为 null
         */
        @Override
        public Object getTarget() {
            return null;
        }

        /**
         * 返回方法参数。
         *
         * @return 方法参数数组
         */
        @Override
        public Object[] getArgs() {
            return args;
        }

        /**
         * 返回方法签名。
         *
         * @return 始终为 null
         */
        @Override
        public Signature getSignature() {
            return null;
        }

        /**
         * 返回源码位置信息。
         *
         * @return 始终为 null
         */
        @Override
        public SourceLocation getSourceLocation() {
            return null;
        }

        /**
         * 返回切点类型。
         *
         * @return 始终为 null
         */
        @Override
        public String getKind() {
            return null;
        }

        /**
         * 返回静态切点信息。
         *
         * @return 始终为 null
         */
        @Override
        public StaticPart getStaticPart() {
            return null;
        }

        /**
         * 返回简短字符串表示。
         *
         * @return 固定字符串
         */
        @Override
        public String toShortString() {
            return "static";
        }

        /**
         * 返回完整字符串表示。
         *
         * @return 固定字符串
         */
        @Override
        public String toLongString() {
            return "static";
        }

        /**
         * 设置 around 闭包。
         *
         * @param arc around 闭包
         */
        @Override
        public void set$AroundClosure(org.aspectj.runtime.internal.AroundClosure arc) {
        }
    }
}
