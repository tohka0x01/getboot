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
package com.getboot.web.api.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getboot.support.api.trace.TraceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一响应测试。
 *
 * @author qiheng
 */
class ApiResponseTest {

    /**
     * JSON 序列化工具。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 清理测试写入的 Trace 上下文。
     */
    @AfterEach
    void tearDown() {
        TraceContextHolder.clear();
    }

    /**
     * 验证默认成功响应。
     */
    @Test
    void shouldCreateDefaultSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertTrue(response.isSuccess());
        assertFalse(response.isFail());
        assertEquals(ApiResponse.SUCCESS_STATUS, response.getStatus());
        assertEquals(ApiResponse.SUCCESS_CODE, response.getCode());
        assertEquals("ok", response.getData());
    }

    /**
     * 验证默认失败响应。
     */
    @Test
    void shouldCreateDefaultFailResponse() {
        ApiResponse<Void> response = ApiResponse.fail();

        assertFalse(response.isSuccess());
        assertTrue(response.isFail());
        assertEquals(ApiResponse.FAIL_STATUS, response.getStatus());
        assertEquals(ApiResponse.SYSTEM_ERROR_CODE, response.getCode());
    }

    /**
     * 验证响应元信息链式设置。
     */
    @Test
    void shouldUpdateMetaInfo() {
        ApiResponse<Void> response = ApiResponse.<Void>success()
                .setTraceId("trace-123")
                .setCostMillis(25L);

        assertNotNull(response.getMeta());
        assertEquals("trace-123", response.getMeta().getTraceId());
        assertEquals(25L, response.getMeta().getCostMillis());
    }

    /**
     * 验证创建响应时会自动带上当前链路 TraceId。
     */
    @Test
    void shouldCreateResponseWithCurrentTraceId() {
        TraceContextHolder.bindTraceId("abc123xyz");

        ApiResponse<String> response = ApiResponse.success("ok");

        assertNotNull(response.getMeta());
        assertEquals("abc123xyz", response.getMeta().getTraceId());
    }

    /**
     * 验证旧版调试方法仍能映射到新元信息字段。
     */
    @Test
    void shouldKeepDeprecatedDebugHelpersCompatible() {
        ApiResponse<Void> response = ApiResponse.<Void>success()
                .setTid("trace-compat")
                .setCost(12L);

        assertNotNull(response.getDebug());
        assertEquals("trace-compat", response.getMeta().getTraceId());
        assertEquals(12L, response.getMeta().getCostMillis());
    }

    /**
     * 验证自定义成功状态码仍会被视为成功响应。
     */
    @Test
    void shouldTreatCustomSuccessCodeAsSuccess() {
        ApiResponse<Void> response = ApiResponse.success(201, "created");

        assertTrue(response.isSuccess());
        assertFalse(response.isFail());
        assertEquals(ApiResponse.SUCCESS_STATUS, response.getStatus());
        assertEquals(201, response.getCode());
        assertEquals("created", response.getMessage());
    }

    /**
     * 验证带数据和自定义状态码的失败响应会保留响应上下文。
     */
    @Test
    void shouldCreateFailResponseWithDataAndCustomCode() {
        ApiResponse<String> response = ApiResponse.fail("payload", 409, "conflict");

        assertTrue(response.isFail());
        assertEquals(ApiResponse.FAIL_STATUS, response.getStatus());
        assertEquals(409, response.getCode());
        assertEquals("payload", response.getData());
        assertEquals("conflict", response.getMessage());
    }

    /**
     * 验证未设置元信息时不会输出额外的元信息节点。
     *
     * @throws Exception 序列化异常
     */
    @Test
    void shouldOmitMetaWhenMetaIsNotInitialized() throws Exception {
        ApiResponse<String> response = ApiResponse.success("ok");

        String json = OBJECT_MAPPER.writeValueAsString(response);

        assertNotNull(json);
        assertFalse(json.contains("\"success\":"));
        assertFalse(json.contains("\"fail\":"));
        assertTrue(json.contains("\"status\":\"success\""));
        assertFalse(json.contains("\"meta\""));
    }

    /**
     * 验证响应元信息会按新协议字段序列化。
     *
     * @throws Exception 序列化异常
     */
    @Test
    void shouldSerializeMetaInsteadOfLegacyDebugField() throws Exception {
        ApiResponse<String> response = ApiResponse.<String>success("ok")
                .setTraceId("trace-123")
                .setCostMillis(25L);

        String json = OBJECT_MAPPER.writeValueAsString(response);

        assertTrue(json.contains("\"meta\""));
        assertTrue(json.contains("\"traceId\":\"trace-123\""));
        assertTrue(json.contains("\"costMillis\":25"));
        assertFalse(json.contains("\"timestamp\""));
        assertFalse(json.contains("\"debug\""));
    }

    /**
     * 验证非法状态值会被拒绝。
     */
    @Test
    void shouldRejectUnsupportedStatusValue() {
        ApiResponse<Void> response = ApiResponse.success();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> response.setStatus("partial"));

        assertEquals("Response status must be either success or fail.", exception.getMessage());
    }
}
