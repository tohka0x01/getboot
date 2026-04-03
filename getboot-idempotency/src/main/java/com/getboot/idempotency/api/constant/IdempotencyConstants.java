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
package com.getboot.idempotency.api.constant;

/**
 * 幂等常量定义。
 *
 * @author qiheng
 */
public final class IdempotencyConstants {

    /**
     * 工具类私有构造方法。
     */
    private IdempotencyConstants() {
    }

    /**
     * Redis 存储类型。
     */
    public static final String STORE_TYPE_REDIS = "redis";

    /**
     * 表示使用默认 TTL 的占位值。
     */
    public static final long USE_DEFAULT_TTL_SECONDS = -1L;

    /**
     * 默认 TTL，单位秒。
     */
    public static final long DEFAULT_TTL_SECONDS = 300L;
}
