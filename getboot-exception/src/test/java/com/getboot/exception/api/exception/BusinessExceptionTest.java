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

    /**
     * 验证通过错误码、消息和原因创建异常时会保留原因异常。
     */
    @Test
    void shouldKeepCauseWhenCreatedFromCodeMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("root cause");

        BusinessException exception = new BusinessException(500, "system error", cause);

        assertEquals(500, exception.getErrorCodeValue());
        assertEquals("system error", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    /**
     * 验证自定义消息加错误码构造器会保留错误码并覆盖异常消息。
     */
    @Test
    void shouldOverrideMessageWhenCreatedWithCustomMessageAndErrorCode() {
        BusinessException exception = new BusinessException("custom token expired", CommonErrorCode.TOKEN_EXPIRED);

        assertSame(CommonErrorCode.TOKEN_EXPIRED, exception.getErrorCode());
        assertEquals("custom token expired", exception.getMessage());
        assertEquals("401:Login session expired. Please sign in again. - custom token expired", exception.getFullMessage());
    }

    /**
     * 验证普通消息与原因构造器不会附带错误码，但会保留原因异常。
     */
    @Test
    void shouldKeepPlainMessageAndCauseWithoutErrorCode() {
        IllegalArgumentException cause = new IllegalArgumentException("bad request");

        BusinessException exception = new BusinessException("custom error", cause);

        assertNull(exception.getErrorCode());
        assertEquals("custom error", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
