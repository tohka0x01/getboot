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
package com.getboot.idempotency.api.properties;

import com.getboot.idempotency.api.constant.IdempotencyConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 幂等配置属性。
 *
 * @author qiheng
 */
@Data
@ConfigurationProperties(prefix = "getboot.idempotency")
public class IdempotencyProperties {

    /**
     * 是否启用幂等能力。
     */
    private boolean enabled = true;

    /**
     * 当前启用的幂等存储类型。
     */
    private String type = IdempotencyConstants.STORE_TYPE_REDIS;

    /**
     * 默认幂等记录存活时间，单位秒。
     */
    private long defaultTtlSeconds = IdempotencyConstants.DEFAULT_TTL_SECONDS;

    /**
     * Redis 幂等配置。
     */
    private Redis redis = new Redis();

    /**
     * 解析最终使用的 key 前缀。
     *
     * @return key 前缀
     */
    public String resolveKeyPrefix() {
        return redis.getKeyPrefix();
    }

    /**
     * Redis 幂等配置项。
     */
    @Data
    public static class Redis {

        /**
         * 是否启用 Redis 存储实现。
         */
        private boolean enabled = true;

        /**
         * Redis 幂等 key 前缀。
         */
        private String keyPrefix = "getboot:idempotency";
    }
}
