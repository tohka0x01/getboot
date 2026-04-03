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
package com.getboot.ai.infrastructure.openai.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getboot.ai.api.model.AiChatMessage;
import com.getboot.ai.api.model.AiMessageRole;
import com.getboot.ai.api.properties.AiProperties;
import com.getboot.ai.api.request.AiChatRequest;
import com.getboot.ai.api.request.AiEmbeddingRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * OpenAI 模型客户端测试。
 *
 * @author qiheng
 */
class OpenAiAiModelClientTest {

    /**
     * Jackson 映射器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证聊天响应能够正确映射。
     *
     * @throws Exception JSON 解析异常
     */
    @Test
    void shouldMapChatResponse() throws Exception {
        OpenAiRestGateway gateway = mock(OpenAiRestGateway.class);
        when(gateway.createResponse(any())).thenReturn(objectMapper.readTree("""
                {
                  "id": "resp_001",
                  "model": "gpt-5-mini",
                  "status": "completed",
                  "output_text": "Hello from OpenAI",
                  "usage": {
                    "input_tokens": 12,
                    "output_tokens": 7,
                    "total_tokens": 19
                  }
                }
                """));

        OpenAiAiModelClient client = new OpenAiAiModelClient(gateway, aiProperties());
        AiChatRequest request = new AiChatRequest();
        request.setMessages(List.of(new AiChatMessage(AiMessageRole.USER, "Hello")));

        var response = client.chat(request);

        assertEquals("resp_001", response.getResponseId());
        assertEquals("gpt-5-mini", response.getModel());
        assertEquals("Hello from OpenAI", response.getContent());
        assertEquals(19, response.getTotalTokens());
    }

    /**
     * 验证向量响应能够正确映射。
     *
     * @throws Exception JSON 解析异常
     */
    @Test
    void shouldMapEmbeddingResponse() throws Exception {
        OpenAiRestGateway gateway = mock(OpenAiRestGateway.class);
        when(gateway.createEmbeddings(any())).thenReturn(objectMapper.readTree("""
                {
                  "model": "text-embedding-3-small",
                  "data": [
                    {
                      "index": 0,
                      "embedding": [0.1, 0.2, 0.3]
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 9,
                    "total_tokens": 9
                  }
                }
                """));

        OpenAiAiModelClient client = new OpenAiAiModelClient(gateway, aiProperties());
        AiEmbeddingRequest request = new AiEmbeddingRequest();
        request.setInputs(List.of("GetBoot"));

        var response = client.embed(request);

        assertEquals("text-embedding-3-small", response.getModel());
        assertEquals(1, response.getItems().size());
        assertEquals(0.2D, response.getItems().get(0).getVector().get(1));
        assertEquals(9, response.getTotalTokens());
    }

    /**
     * 构造测试使用的 AI 配置。
     *
     * @return AI 配置
     */
    private AiProperties aiProperties() {
        AiProperties properties = new AiProperties();
        properties.getOpenai().setApiKey("sk-test");
        properties.getOpenai().setDefaultChatModel("gpt-5-mini");
        properties.getOpenai().setDefaultEmbeddingModel("text-embedding-3-small");
        return properties;
    }
}
