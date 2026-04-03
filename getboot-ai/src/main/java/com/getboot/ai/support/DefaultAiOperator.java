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

import com.getboot.ai.api.exception.AiException;
import com.getboot.ai.api.model.AiChatMessage;
import com.getboot.ai.api.model.AiMessageRole;
import com.getboot.ai.api.operator.AiOperator;
import com.getboot.ai.api.request.AiChatRequest;
import com.getboot.ai.api.request.AiEmbeddingRequest;
import com.getboot.ai.api.request.AiRerankRequest;
import com.getboot.ai.api.response.AiChatResponse;
import com.getboot.ai.api.response.AiEmbeddingItem;
import com.getboot.ai.api.response.AiEmbeddingResponse;
import com.getboot.ai.api.response.AiRerankItem;
import com.getboot.ai.api.response.AiRerankResponse;
import com.getboot.ai.spi.AiModelClient;
import com.getboot.ai.spi.PromptTemplateRenderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 默认 AI 门面实现。
 *
 * @author qiheng
 */
public class DefaultAiOperator implements AiOperator {

    /**
     * 模型客户端。
     */
    private final AiModelClient modelClient;

    /**
     * 提示词模板渲染器。
     */
    private final PromptTemplateRenderer promptTemplateRenderer;

    /**
     * 创建默认 AI 门面实现。
     *
     * @param modelClient 模型客户端
     * @param promptTemplateRenderer 提示词模板渲染器
     */
    public DefaultAiOperator(AiModelClient modelClient, PromptTemplateRenderer promptTemplateRenderer) {
        this.modelClient = modelClient;
        this.promptTemplateRenderer = promptTemplateRenderer;
    }

    /**
     * 执行聊天请求。
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    @Override
    public AiChatResponse chat(AiChatRequest request) {
        if (request == null) {
            throw new AiException("AI chat request must not be null.");
        }
        return modelClient.chat(resolveChatRequest(request));
    }

    /**
     * 执行向量化请求。
     *
     * @param request 向量化请求
     * @return 向量化响应
     */
    @Override
    public AiEmbeddingResponse embed(AiEmbeddingRequest request) {
        if (request == null) {
            throw new AiException("AI embedding request must not be null.");
        }
        AiEmbeddingRequest resolvedRequest = new AiEmbeddingRequest();
        resolvedRequest.setModel(request.getModel());
        resolvedRequest.setInputs(AiSupport.requireTextList(request.getInputs(), "Embedding inputs"));
        return modelClient.embed(resolvedRequest);
    }

    /**
     * 执行文本重排请求。
     *
     * @param request 重排请求
     * @return 重排响应
     */
    @Override
    public AiRerankResponse rerank(AiRerankRequest request) {
        if (request == null) {
            throw new AiException("AI rerank request must not be null.");
        }
        String query = AiSupport.requireText(request.getQuery(), "Rerank query");
        List<String> documents = AiSupport.requireTextList(request.getDocuments(), "Rerank documents");

        AiEmbeddingRequest embeddingRequest = new AiEmbeddingRequest();
        embeddingRequest.setModel(request.getEmbeddingModel());

        List<String> inputs = new ArrayList<>(documents.size() + 1);
        inputs.add(query);
        inputs.addAll(documents);
        embeddingRequest.setInputs(inputs);

        AiEmbeddingResponse embeddingResponse = modelClient.embed(embeddingRequest);
        if (embeddingResponse.getItems().size() != inputs.size()) {
            throw new AiException("Embedding response size does not match rerank input size.");
        }

        AiEmbeddingItem queryEmbedding = embeddingResponse.getItems().get(0);
        List<AiRerankItem> items = new ArrayList<>(documents.size());
        for (int index = 0; index < documents.size(); index++) {
            AiEmbeddingItem documentEmbedding = embeddingResponse.getItems().get(index + 1);
            AiRerankItem item = new AiRerankItem();
            item.setIndex(index);
            item.setDocument(documents.get(index));
            item.setScore(VectorSupport.cosineSimilarity(queryEmbedding.getVector(), documentEmbedding.getVector()));
            items.add(item);
        }

        items.sort(Comparator.comparing(AiRerankItem::getScore).reversed());
        int topK = AiSupport.normalizeTopK(request.getTopK(), items.size());

        AiRerankResponse response = new AiRerankResponse();
        response.setEmbeddingModel(embeddingResponse.getModel());
        response.setItems(new ArrayList<>(items.subList(0, topK)));
        return response;
    }

    /**
     * 解析聊天请求。
     *
     * @param request 原始聊天请求
     * @return 解析后的聊天请求
     */
    private AiChatRequest resolveChatRequest(AiChatRequest request) {
        List<AiChatMessage> resolvedMessages = new ArrayList<>();
        for (AiChatMessage message : request.getMessages()) {
            if (message == null) {
                continue;
            }
            AiMessageRole role = message.getRole() == null ? AiMessageRole.USER : message.getRole();
            String content = AiSupport.requireText(message.getContent(), "Chat message content");
            resolvedMessages.add(new AiChatMessage(role, content));
        }

        if (request.getPromptTemplate() != null) {
            String renderedPrompt = AiSupport.requireText(
                    promptTemplateRenderer.render(request.getPromptTemplate()),
                    "Rendered prompt template"
            );
            resolvedMessages.add(new AiChatMessage(AiMessageRole.USER, renderedPrompt));
        }

        if (resolvedMessages.isEmpty()) {
            throw new AiException("AI chat messages must not be empty.");
        }

        AiChatRequest resolvedRequest = new AiChatRequest();
        resolvedRequest.setModel(request.getModel());
        resolvedRequest.setInstructions(request.getInstructions());
        resolvedRequest.setReasoningEffort(request.getReasoningEffort());
        resolvedRequest.setMaxOutputTokens(request.getMaxOutputTokens());
        resolvedRequest.setMessages(resolvedMessages);
        return resolvedRequest;
    }
}
