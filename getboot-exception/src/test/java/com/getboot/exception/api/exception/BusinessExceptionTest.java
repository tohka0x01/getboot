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
package com.getboot.exception.api.exception;

import com.getboot.exception.api.code.CommonErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link BusinessException} 测试。
 *
 * @author qiheng
 */
class BusinessExceptionTest {

    /**
     * 验证通过错误码与消息创建异常时会保留完整上下文。
     */
    @Test
    void shouldCreateExceptionFromCodeAndMessage() {
        BusinessException exception = BusinessException.of(422, "invalid request");

        assertEquals(422, exception.getErrorCodeValue());
        assertEquals("invalid request", exception.getMessage());
        assertEquals("422:invalid request - invalid request", exception.getFullMessage());
    }

    /**
     * 验证直接传入错误码对象时会保留原始实例。
     */
    @Test
    void shouldKeepProvidedErrorCodeInstance() {
        BusinessException exception = BusinessException.of(CommonErrorCode.NOT_FOUND);

        assertSame(CommonErrorCode.NOT_FOUND, exception.getErrorCode());
        assertEquals(404, exception.getErrorCodeValue());
        assertEquals("The requested resource was not found.", exception.getMessage());
    }

    /**
     * 验证仅传入普通消息时错误码对象为空。
     */
    @Test
    void shouldReturnNullErrorCodeWhenCreatedWithPlainMessage() {
        BusinessException exception = new BusinessException("custom error");

        assertNull(exception.getErrorCodeValue());
        assertEquals("custom error", exception.getFullMessage());
    }
}
