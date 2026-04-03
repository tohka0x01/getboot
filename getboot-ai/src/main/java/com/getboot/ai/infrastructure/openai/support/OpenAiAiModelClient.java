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

import com.fasterxml.jackson.databind.JsonNode;
import com.getboot.ai.api.properties.AiProperties;
import com.getboot.ai.api.request.AiChatRequest;
import com.getboot.ai.api.request.AiEmbeddingRequest;
import com.getboot.ai.api.response.AiChatResponse;
import com.getboot.ai.api.response.AiEmbeddingItem;
import com.getboot.ai.api.response.AiEmbeddingResponse;
import com.getboot.ai.spi.AiModelClient;
import com.getboot.ai.support.AiSupport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 模型客户端。
 *
 * @author qiheng
 */
public class OpenAiAiModelClient implements AiModelClient {

    /**
     * OpenAI 请求网关。
     */
    private final OpenAiRestGateway gateway;

    /**
     * AI 模块配置。
     */
    private final AiProperties properties;

    /**
     * 创建 OpenAI 模型客户端。
     *
     * @param gateway OpenAI 请求网关
     * @param properties AI 模块配置
     */
    public OpenAiAiModelClient(OpenAiRestGateway gateway, AiProperties properties) {
        this.gateway = gateway;
        this.properties = properties;
    }

    /**
     * 执行聊天请求。
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    @Override
    public AiChatResponse chat(AiChatRequest request) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", AiSupport.resolveModel(
                request.getModel(),
                properties.getOpenai().getDefaultChatModel(),
                "OpenAI defaultChatModel"
        ));
        if (AiSupport.hasText(request.getInstructions())) {
            requestBody.put("instructions", request.getInstructions().trim());
        }
        if (AiSupport.hasText(request.getReasoningEffort())) {
            requestBody.put("reasoning", Map.of("effort", request.getReasoningEffort().trim()));
        } else if (AiSupport.hasText(properties.getOpenai().getDefaultReasoningEffort())) {
            requestBody.put("reasoning", Map.of("effort", properties.getOpenai().getDefaultReasoningEffort().trim()));
        }
        if (request.getMaxOutputTokens() != null && request.getMaxOutputTokens() > 0) {
            requestBody.put("max_output_tokens", request.getMaxOutputTokens());
        }
        requestBody.put("input", buildMessageInputs(request));

        return mapChatResponse(gateway.createResponse(requestBody));
    }

    /**
     * 执行向量化请求。
     *
     * @param request 向量化请求
     * @return 向量化响应
     */
    @Override
    public AiEmbeddingResponse embed(AiEmbeddingRequest request) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", AiSupport.resolveModel(
                request.getModel(),
                properties.getOpenai().getDefaultEmbeddingModel(),
                "OpenAI defaultEmbeddingModel"
        ));
        requestBody.put("input", request.getInputs());

        return mapEmbeddingResponse(gateway.createEmbeddings(requestBody));
    }

    /**
     * 构造消息输入列表。
     *
     * @param request 聊天请求
     * @return 消息输入列表
     */
    private List<Map<String, Object>> buildMessageInputs(AiChatRequest request) {
        List<Map<String, Object>> inputs = new ArrayList<>(request.getMessages().size());
        request.getMessages().forEach(message -> {
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("role", message.getRole().getApiValue());
            input.put("content", message.getContent());
            inputs.add(input);
        });
        return inputs;
    }

    /**
     * 映射聊天响应。
     *
     * @param responseNode OpenAI 响应
     * @return 聊天响应
     */
    private AiChatResponse mapChatResponse(JsonNode responseNode) {
        AiChatResponse response = new AiChatResponse();
        response.setResponseId(textValue(responseNode, "id"));
        response.setModel(textValue(responseNode, "model"));
        response.setStatus(textValue(responseNode, "status"));
        response.setContent(resolveOutputText(responseNode));
        response.setInputTokens(intValue(responseNode.path("usage"), "input_tokens"));
        response.setOutputTokens(intValue(responseNode.path("usage"), "output_tokens"));
        response.setTotalTokens(intValue(responseNode.path("usage"), "total_tokens"));
        return response;
    }

    /**
     * 映射向量化响应。
     *
     * @param responseNode OpenAI 响应
     * @return 向量化响应
     */
    private AiEmbeddingResponse mapEmbeddingResponse(JsonNode responseNode) {
        AiEmbeddingResponse response = new AiEmbeddingResponse();
        response.setModel(textValue(responseNode, "model"));
        response.setPromptTokens(intValue(responseNode.path("usage"), "prompt_tokens"));
        response.setTotalTokens(intValue(responseNode.path("usage"), "total_tokens"));

        List<AiEmbeddingItem> items = new ArrayList<>();
        for (JsonNode itemNode : responseNode.path("data")) {
            AiEmbeddingItem item = new AiEmbeddingItem();
            item.setIndex(itemNode.path("index").isMissingNode() ? null : itemNode.path("index").asInt());
            List<Double> vector = new ArrayList<>();
            for (JsonNode vectorNode : itemNode.path("embedding")) {
                vector.add(vectorNode.asDouble());
            }
            item.setVector(vector);
            items.add(item);
        }
        response.setItems(items);
        return response;
    }

    /**
     * 解析响应文本。
     *
     * @param responseNode OpenAI 响应
     * @return 响应文本
     */
    private String resolveOutputText(JsonNode responseNode) {
        if (responseNode.path("output_text").isTextual()) {
            return responseNode.path("output_text").asText();
        }

        StringBuilder contentBuilder = new StringBuilder();
        for (JsonNode outputNode : responseNode.path("output")) {
            for (JsonNode contentNode : outputNode.path("content")) {
                if (contentNode.path("text").isTextual()) {
                    if (contentBuilder.length() > 0) {
                        contentBuilder.append('\n');
                    }
                    contentBuilder.append(contentNode.path("text").asText());
                }
            }
        }
        return contentBuilder.toString();
    }

    /**
     * 读取文本字段。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @return 文本值
     */
    private String textValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        return fieldNode.isMissingNode() || fieldNode.isNull() ? null : fieldNode.asText();
    }

    /**
     * 读取整数值字段。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @return 整数值
     */
    private Integer intValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        return fieldNode.isMissingNode() || fieldNode.isNull() ? null : fieldNode.asInt();
    }
}
