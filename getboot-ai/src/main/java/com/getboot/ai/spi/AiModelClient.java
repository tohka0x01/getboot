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
package com.getboot.ai.spi;

import com.getboot.ai.api.request.AiChatRequest;
import com.getboot.ai.api.request.AiEmbeddingRequest;
import com.getboot.ai.api.response.AiChatResponse;
import com.getboot.ai.api.response.AiEmbeddingResponse;

/**
 * AI 模型客户端。
 *
 * @author qiheng
 */
public interface AiModelClient {

    /**
     * 执行聊天请求。
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    AiChatResponse chat(AiChatRequest request);

    /**
     * 执行向量化请求。
     *
     * @param request 向量化请求
     * @return 向量化响应
     */
    AiEmbeddingResponse embed(AiEmbeddingRequest request);
}
