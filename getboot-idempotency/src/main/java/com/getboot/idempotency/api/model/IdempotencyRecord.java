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
package com.getboot.idempotency.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 幂等调用记录。
 *
 * @author qiheng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord implements Serializable {

    /**
     * 幂等记录状态。
     */
    private IdempotencyStatus status;

    /**
     * 首次成功执行结果。
     */
    private Object result;

    /**
     * 创建处理中记录。
     *
     * @return 处理中记录
     */
    public static IdempotencyRecord processing() {
        return new IdempotencyRecord(IdempotencyStatus.PROCESSING, null);
    }

    /**
     * 创建已完成记录。
     *
     * @param result 执行结果
     * @return 已完成记录
     */
    public static IdempotencyRecord completed(Object result) {
        return new IdempotencyRecord(IdempotencyStatus.COMPLETED, result);
    }
}
