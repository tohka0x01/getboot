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
package com.getboot.idempotency.api.annotation;

import com.getboot.idempotency.api.constant.IdempotencyConstants;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级幂等注解。
 *
 * @author qiheng
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等场景名。
     *
     * <p>为空时回退到“类名.方法名”。</p>
     *
     * @return 场景名
     */
    String scene() default "";

    /**
     * 固定幂等 key。
     *
     * <p>配置后优先级高于 {@link #keyExpression()}。</p>
     *
     * @return 固定 key
     */
    String key() default "";

    /**
     * 用于解析幂等 key 的 SpEL 表达式。
     *
     * @return SpEL 表达式
     */
    String keyExpression() default "";

    /**
     * 结果缓存 TTL，单位秒。
     *
     * <p>使用 {@code -1} 表示跟随 {@code getboot.idempotency.default-ttl-seconds}。</p>
     *
     * @return TTL 秒数
     */
    long ttlSeconds() default IdempotencyConstants.USE_DEFAULT_TTL_SECONDS;

    /**
     * 同一 key 仍在处理中时使用的提示消息。
     *
     * @return 重复处理中提示
     */
    String message() default "Duplicate request. Please do not submit repeatedly.";
}
