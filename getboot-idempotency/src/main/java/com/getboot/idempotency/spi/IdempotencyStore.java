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

import com.getboot.idempotency.api.model.IdempotencyRecord;

import java.time.Duration;

/**
 * 幂等存储 SPI。
 *
 * @author qiheng
 */
public interface IdempotencyStore {

    /**
     * 读取幂等记录。
     *
     * @param key 幂等 key
     * @return 幂等记录
     */
    IdempotencyRecord get(String key);

    /**
     * 将指定 key 标记为处理中。
     *
     * @param key 幂等 key
     * @param ttl 记录 TTL
     * @return 是否标记成功
     */
    boolean markProcessing(String key, Duration ttl);

    /**
     * 将指定 key 标记为已完成。
     *
     * @param key 幂等 key
     * @param result 执行结果
     * @param ttl 记录 TTL
     */
    void markCompleted(String key, Object result, Duration ttl);

    /**
     * 删除幂等记录。
     *
     * @param key 幂等 key
     */
    void delete(String key);
}
