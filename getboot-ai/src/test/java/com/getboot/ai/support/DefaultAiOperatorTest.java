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
package com.getboot.ai.support;

import com.getboot.ai.api.model.AiChatMessage;
import com.getboot.ai.api.model.AiMessageRole;
import com.getboot.ai.api.prompt.PromptTemplate;
import com.getboot.ai.api.request.AiChatRequest;
import com.getboot.ai.api.request.AiEmbeddingRequest;
import com.getboot.ai.api.request.AiRerankRequest;
import com.getboot.ai.api.response.AiChatResponse;
import com.getboot.ai.api.response.AiEmbeddingItem;
import com.getboot.ai.api.response.AiEmbeddingResponse;
import com.getboot.ai.spi.AiModelClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 默认 AI 门面测试。
 *
 * @author qiheng
 */
class DefaultAiOperatorTest {

    /**
     * 验证聊天请求会渲染提示词模板并传给模型客户端。
     */
    @Test
    void shouldRenderPromptTemplateBeforeChat() {
        AiModelClient modelClient = mock(AiModelClient.class);
        AiChatResponse chatResponse = new AiChatResponse();
        chatResponse.setContent("done");
        when(modelClient.chat(any(AiChatRequest.class))).thenReturn(chatResponse);

        DefaultAiOperator operator = new DefaultAiOperator(modelClient, new DefaultPromptTemplateRenderer());
        PromptTemplate promptTemplate = new PromptTemplate();
        promptTemplate.setContent("Summarize {{title}}");
        promptTemplate.setVariables(Map.of("title", "GetBoot"));

        AiChatRequest request = new AiChatRequest();
        request.setInstructions("You are helpful.");
        request.setMessages(List.of(new AiChatMessage(AiMessageRole.SYSTEM, "reply in Chinese")));
        request.setPromptTemplate(promptTemplate);

        AiChatResponse response = operator.chat(request);

        ArgumentCaptor<AiChatRequest> captor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(modelClient).chat(captor.capture());
        assertEquals("done", response.getContent());
        assertEquals(2, captor.getValue().getMessages().size());
        assertEquals("Summarize GetBoot", captor.getValue().getMessages().get(1).getContent());
    }

    /**
     * 验证重排会按余弦相似度降序返回结果。
     */
    @Test
    void shouldRerankDocumentsByEmbeddingSimilarity() {
        AiModelClient modelClient = mock(AiModelClient.class);
        AiEmbeddingResponse embeddingResponse = new AiEmbeddingResponse();
        embeddingResponse.setModel("text-embedding-3-small");
        embeddingResponse.setItems(List.of(
                embeddingItem(0, List.of(1D, 0D)),
                embeddingItem(1, List.of(0.9D, 0.1D)),
                embeddingItem(2, List.of(0D, 1D))
        ));
        when(modelClient.embed(any(AiEmbeddingRequest.class))).thenReturn(embeddingResponse);

        DefaultAiOperator operator = new DefaultAiOperator(modelClient, new DefaultPromptTemplateRenderer());
        AiRerankRequest request = new AiRerankRequest();
        request.setQuery("query");
        request.setDocuments(List.of("doc-a", "doc-b"));
        request.setTopK(1);

        var response = operator.rerank(request);

        assertEquals("text-embedding-3-small", response.getEmbeddingModel());
        assertEquals(1, response.getItems().size());
        assertEquals("doc-a", response.getItems().get(0).getDocument());
    }

    /**
     * 构造测试使用的向量项。
     *
     * @param index 结果序号
     * @param vector 向量值
     * @return 向量项
     */
    private AiEmbeddingItem embeddingItem(int index, List<Double> vector) {
        AiEmbeddingItem item = new AiEmbeddingItem();
        item.setIndex(index);
        item.setVector(vector);
        return item;
    }
}
