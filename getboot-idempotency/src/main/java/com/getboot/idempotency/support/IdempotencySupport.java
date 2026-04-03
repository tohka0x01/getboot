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
import com.getboot.idempotency.api.constant.IdempotencyConstants;
import com.getboot.idempotency.api.exception.IdempotencyException;
import com.getboot.idempotency.spi.IdempotencyKeyResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 幂等公共辅助方法。
 *
 * @author qiheng
 */
public final class IdempotencySupport {

    /**
     * 工具类私有构造方法。
     */
    private IdempotencySupport() {
    }

    /**
     * 解析代理对象背后的最具体方法。
     *
     * @param joinPoint 切点对象
     * @return 目标方法
     */
    public static Method resolveMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object target = joinPoint.getTarget();
        if (target == null) {
            return method;
        }
        return AopUtils.getMostSpecificMethod(method, target.getClass());
    }

    /**
     * 解析完整幂等 key。
     *
     * @param joinPoint 切点对象
     * @param method 目标方法
     * @param idempotent 幂等注解
     * @param keyResolver 幂等 key 解析器
     * @param keyPrefix key 前缀
     * @return 完整幂等 key
     */
    public static String resolveFullKey(ProceedingJoinPoint joinPoint,
                                        Method method,
                                        Idempotent idempotent,
                                        IdempotencyKeyResolver keyResolver,
                                        String keyPrefix) {
        String resolvedKey = keyResolver.resolve(joinPoint, method, idempotent);
        String scene = resolveScene(method, idempotent);
        return buildFullKey(keyPrefix, scene, resolvedKey);
    }

    /**
     * 解析幂等场景名。
     *
     * @param method 目标方法
     * @param idempotent 幂等注解
     * @return 场景名
     */
    public static String resolveScene(Method method, Idempotent idempotent) {
        if (StringUtils.hasText(idempotent.scene())) {
            return idempotent.scene().trim();
        }
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    /**
     * 拼接完整幂等 key。
     *
     * @param keyPrefix key 前缀
     * @param scene 幂等场景
     * @param resolvedKey 已解析业务 key
     * @return 完整幂等 key
     */
    public static String buildFullKey(String keyPrefix, String scene, String resolvedKey) {
        if (!StringUtils.hasText(keyPrefix)) {
            throw new IdempotencyException("Idempotent key prefix must not be empty.");
        }
        if (!StringUtils.hasText(scene)) {
            throw new IdempotencyException("Idempotent scene must not be empty.");
        }
        if (!StringUtils.hasText(resolvedKey)) {
            throw new IdempotencyException("Resolved idempotent key must not be empty.");
        }
        return keyPrefix + ":" + scene + "#" + resolvedKey;
    }

    /**
     * 解析最终使用的 TTL。
     *
     * @param idempotent 幂等注解
     * @param defaultTtlSeconds 默认 TTL
     * @return TTL 秒数
     */
    public static long resolveTtlSeconds(Idempotent idempotent, long defaultTtlSeconds) {
        long ttlSeconds = idempotent.ttlSeconds();
        if (ttlSeconds == IdempotencyConstants.USE_DEFAULT_TTL_SECONDS) {
            ttlSeconds = defaultTtlSeconds;
        }
        if (ttlSeconds <= 0) {
            throw new IdempotencyException("Idempotent ttlSeconds must be positive.");
        }
        return ttlSeconds;
    }
}
