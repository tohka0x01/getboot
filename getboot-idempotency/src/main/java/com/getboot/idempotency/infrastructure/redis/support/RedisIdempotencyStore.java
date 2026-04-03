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
package com.getboot.idempotency.infrastructure.redis.support;

import com.getboot.idempotency.api.model.IdempotencyRecord;
import com.getboot.idempotency.spi.IdempotencyStore;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;

import java.time.Duration;

/**
 * 基于 Redis 的幂等存储。
 *
 * @author qiheng
 */
public class RedisIdempotencyStore implements IdempotencyStore {

    /**
     * Redis 模板。
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 创建 Redis 幂等存储。
     *
     * @param redisTemplate Redis 模板
     */
    public RedisIdempotencyStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 读取幂等记录。
     *
     * @param key 幂等 key
     * @return 幂等记录
     */
    @Override
    public IdempotencyRecord get(String key) {
        assertKey(key);
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof IdempotencyRecord record) {
            return record;
        }
        throw new IllegalStateException("Unexpected idempotency record type: " + value.getClass().getName());
    }

    /**
     * 将指定 key 标记为处理中。
     *
     * @param key 幂等 key
     * @param ttl 记录 TTL
     * @return 是否标记成功
     */
    @Override
    public boolean markProcessing(String key, Duration ttl) {
        assertKey(key);
        assertDuration(ttl);
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, IdempotencyRecord.processing(), ttl);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 将指定 key 标记为已完成。
     *
     * @param key 幂等 key
     * @param result 执行结果
     * @param ttl 记录 TTL
     */
    @Override
    public void markCompleted(String key, Object result, Duration ttl) {
        assertKey(key);
        assertDuration(ttl);
        redisTemplate.opsForValue().set(key, IdempotencyRecord.completed(result), ttl);
    }

    /**
     * 删除幂等记录。
     *
     * @param key 幂等 key
     */
    @Override
    public void delete(String key) {
        assertKey(key);
        redisTemplate.delete(key);
    }

    /**
     * 校验幂等 key。
     *
     * @param key 幂等 key
     */
    private void assertKey(String key) {
        Assert.hasText(key, "Idempotency key must not be blank.");
    }

    /**
     * 校验 TTL。
     *
     * @param ttl 记录 TTL
     */
    private void assertDuration(Duration ttl) {
        Assert.notNull(ttl, "Idempotency ttl must not be null.");
        Assert.isTrue(!ttl.isNegative() && !ttl.isZero(), "Idempotency ttl must be greater than 0.");
    }
}
