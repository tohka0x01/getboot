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
import com.getboot.idempotency.api.model.IdempotencyRecord;

/**
 * 重复幂等请求处理器 SPI。
 *
 * @author qiheng
 */
public interface IdempotencyDuplicateRequestHandler {

    /**
     * 处理重复请求。
     *
     * @param key 幂等 key
     * @param record 幂等记录
     * @param idempotent 幂等注解
     * @return 处理结果
     */
    Object handleDuplicate(String key, IdempotencyRecord record, Idempotent idempotent);
}
