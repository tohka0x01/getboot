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
package com.getboot.lab.controller;

import com.getboot.lab.GetBootLabApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证台接口测试
 *
 * @author qiheng
 */
@SpringBootTest(classes = GetBootLabApplication.class)
@AutoConfigureMockMvc
class LabControllerTest {

    /**
     * MockMvc 测试客户端
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证 Trace 会进入响应头和响应体 meta
     *
     * @throws Exception 请求异常
     */
    @Test
    void shouldReturnTraceIdInHeaderAndBody() throws Exception {
        mockMvc.perform(get("/api/lab/checks/trace").header("X-Trace-Id", "abc123xyz"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "abc123xyz"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.meta.traceId").value("abc123xyz"))
                .andExpect(jsonPath("$.data.contextTraceId").value("abc123xyz"))
                .andExpect(jsonPath("$.data.mdcTraceId").value("abc123xyz"));
    }

    /**
     * 验证 Echo 请求会保持同一个 TraceId
     *
     * @throws Exception 请求异常
     */
    @Test
    void shouldEchoRequestBody() throws Exception {
        mockMvc.perform(post("/api/lab/checks/echo")
                        .header("X-Trace-Id", "echo123abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\",\"attributes\":{\"source\":\"test\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("hello"))
                .andExpect(jsonPath("$.data.attributes.source").value("test"))
                .andExpect(jsonPath("$.data.traceId").value("echo123abc"))
                .andExpect(jsonPath("$.meta.traceId").value("echo123abc"));
    }

    /**
     * 验证业务异常会被 GetBoot Web 统一收口
     *
     * @throws Exception 请求异常
     */
    @Test
    void shouldHandleBusinessException() throws Exception {
        mockMvc.perform(post("/api/lab/checks/business-exception").header("X-Trace-Id", "biz123abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.meta.traceId").value("biz123abc"));
    }
}
