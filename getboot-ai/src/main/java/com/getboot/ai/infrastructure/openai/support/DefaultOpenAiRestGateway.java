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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getboot.ai.api.exception.AiException;
import com.getboot.ai.api.properties.AiProperties;
import com.getboot.ai.support.AiSupport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 默认 OpenAI 请求网关。
 *
 * @author qiheng
 */
public class DefaultOpenAiRestGateway implements OpenAiRestGateway {

    /**
     * OpenAI HTTP 客户端。
     */
    private final HttpClient httpClient;

    /**
     * Jackson 映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * AI 模块配置。
     */
    private final AiProperties properties;

    /**
     * 创建默认 OpenAI 请求网关。
     *
     * @param httpClient OpenAI HTTP 客户端
     * @param objectMapper Jackson 映射器
     * @param properties AI 模块配置
     */
    public DefaultOpenAiRestGateway(HttpClient httpClient, ObjectMapper objectMapper, AiProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 调用 Responses API。
     *
     * @param requestBody 请求体
     * @return 响应体
     */
    @Override
    public JsonNode createResponse(Object requestBody) {
        return post("/responses", requestBody);
    }

    /**
     * 调用 Embeddings API。
     *
     * @param requestBody 请求体
     * @return 响应体
     */
    @Override
    public JsonNode createEmbeddings(Object requestBody) {
        return post("/embeddings", requestBody);
    }

    /**
     * 发送 POST 请求。
     *
     * @param path 接口路径
     * @param requestBody 请求体
     * @return 响应体
     */
    private JsonNode post(String path, Object requestBody) {
        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(resolveEndpoint(path)))
                    .timeout(properties.getOpenai().getReadTimeout())
                    .header("Authorization", "Bearer " + AiSupport.requireText(properties.getOpenai().getApiKey(), "OpenAI apiKey"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8));

            if (AiSupport.hasText(properties.getOpenai().getOrganization())) {
                requestBuilder.header("OpenAI-Organization", properties.getOpenai().getOrganization().trim());
            }
            if (AiSupport.hasText(properties.getOpenai().getProject())) {
                requestBuilder.header("OpenAI-Project", properties.getOpenai().getProject().trim());
            }
            for (Map.Entry<String, String> entry : properties.getOpenai().getDefaultHeaders().entrySet()) {
                if (AiSupport.hasText(entry.getKey()) && AiSupport.hasText(entry.getValue())) {
                    requestBuilder.header(entry.getKey().trim(), entry.getValue().trim());
                }
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiException("OpenAI request failed with status "
                        + response.statusCode()
                        + ": "
                        + truncateBody(response.body()));
            }
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new AiException("OpenAI request failed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiException("OpenAI request interrupted.", exception);
        }
    }

    /**
     * 拼接最终接口地址。
     *
     * @param path 接口路径
     * @return 最终接口地址
     */
    private String resolveEndpoint(String path) {
        String baseUrl = AiSupport.requireText(properties.getOpenai().getBaseUrl(), "OpenAI baseUrl");
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + path;
    }

    /**
     * 截断错误响应体。
     *
     * @param body 原始响应体
     * @return 截断后的响应体
     */
    private String truncateBody(String body) {
        if (!AiSupport.hasText(body)) {
            return "<empty>";
        }
        String normalizedBody = body.trim();
        return normalizedBody.length() <= 512 ? normalizedBody : normalizedBody.substring(0, 512) + "...";
    }
}
