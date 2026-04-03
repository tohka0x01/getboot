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
package com.getboot.idempotency.spi;

import com.getboot.idempotency.api.annotation.Idempotent;
import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

/**
 * 幂等 key 解析器 SPI。
 *
 * @author qiheng
 */
public interface IdempotencyKeyResolver {

    /**
     * 解析幂等 key。
     *
     * @param joinPoint 切点对象
     * @param method 目标方法
     * @param idempotent 幂等注解
     * @return 解析后的幂等 key
     */
    String resolve(ProceedingJoinPoint joinPoint, Method method, Idempotent idempotent);
}
