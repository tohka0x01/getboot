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
package com.getboot.idempotency.support.aop;

import com.getboot.idempotency.api.annotation.Idempotent;
import com.getboot.idempotency.api.exception.IdempotencyException;
import com.getboot.idempotency.api.model.IdempotencyRecord;
import com.getboot.idempotency.api.properties.IdempotencyProperties;
import com.getboot.idempotency.spi.IdempotencyDuplicateRequestHandler;
import com.getboot.idempotency.spi.IdempotencyKeyResolver;
import com.getboot.idempotency.spi.IdempotencyStore;
import com.getboot.idempotency.support.IdempotencySupport;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 方法级幂等切面。
 *
 * @author qiheng
 */
@Aspect
public class IdempotencyAspect {

    /**
     * 幂等存储。
     */
    private final IdempotencyStore idempotencyStore;

    /**
     * 幂等 key 解析器。
     */
    private final IdempotencyKeyResolver idempotencyKeyResolver;

    /**
     * 重复请求处理器。
     */
    private final IdempotencyDuplicateRequestHandler duplicateRequestHandler;

    /**
     * 幂等配置属性。
     */
    private final IdempotencyProperties properties;

    /**
     * 创建方法级幂等切面。
     *
     * @param idempotencyStore 幂等存储
     * @param idempotencyKeyResolver 幂等 key 解析器
     * @param duplicateRequestHandler 重复请求处理器
     * @param properties 幂等配置属性
     */
    public IdempotencyAspect(IdempotencyStore idempotencyStore,
                             IdempotencyKeyResolver idempotencyKeyResolver,
                             IdempotencyDuplicateRequestHandler duplicateRequestHandler,
                             IdempotencyProperties properties) {
        this.idempotencyStore = idempotencyStore;
        this.idempotencyKeyResolver = idempotencyKeyResolver;
        this.duplicateRequestHandler = duplicateRequestHandler;
        this.properties = properties;
    }

    /**
     * 在目标方法执行前后织入幂等控制。
     *
     * @param joinPoint 切点对象
     * @param idempotent 幂等注解
     * @return 目标方法返回值
     * @throws Throwable 目标方法异常
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        Method method = IdempotencySupport.resolveMethod(joinPoint);
        String key = IdempotencySupport.resolveFullKey(
                joinPoint,
                method,
                idempotent,
                idempotencyKeyResolver,
                properties.resolveKeyPrefix()
        );
        Duration ttl = Duration.ofSeconds(
                IdempotencySupport.resolveTtlSeconds(idempotent, properties.getDefaultTtlSeconds())
        );

        IdempotencyRecord existingRecord = idempotencyStore.get(key);
        if (existingRecord != null) {
            return duplicateRequestHandler.handleDuplicate(key, existingRecord, idempotent);
        }

        if (!idempotencyStore.markProcessing(key, ttl)) {
            IdempotencyRecord latestRecord = idempotencyStore.get(key);
            if (latestRecord != null) {
                return duplicateRequestHandler.handleDuplicate(key, latestRecord, idempotent);
            }
            throw new IdempotencyException("Duplicate request detected but state is unavailable. key=" + key);
        }

        try {
            Object result = joinPoint.proceed();
            idempotencyStore.markCompleted(key, result, ttl);
            return result;
        } catch (Throwable ex) {
            idempotencyStore.delete(key);
            throw ex;
        }
    }
}
