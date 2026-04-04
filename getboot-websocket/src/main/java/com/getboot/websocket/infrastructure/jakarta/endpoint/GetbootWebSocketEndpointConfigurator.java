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
package com.getboot.websocket.infrastructure.jakarta.endpoint;

import com.getboot.websocket.api.properties.WebSocketProperties;
import com.getboot.websocket.support.WebSocketSessionAttributes;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 默认 WebSocket Endpoint 配置器。
 *
 * @author qiheng
 */
public class GetbootWebSocketEndpointConfigurator extends ServerEndpointConfig.Configurator {

    /**
     * Spring 管理的 Endpoint 实例。
     */
    private final GetbootWebSocketEndpoint endpoint;

    /**
     * WebSocket 模块配置。
     */
    private final WebSocketProperties properties;

    /**
     * 创建默认 Endpoint 配置器。
     *
     * @param endpoint Spring 管理的 Endpoint 实例
     * @param properties WebSocket 模块配置
     */
    public GetbootWebSocketEndpointConfigurator(GetbootWebSocketEndpoint endpoint, WebSocketProperties properties) {
        this.endpoint = endpoint;
        this.properties = properties;
    }

    /**
     * 校验 Origin。
     *
     * @param originHeaderValue Origin 头值
     * @return 是否允许
     */
    @Override
    public boolean checkOrigin(String originHeaderValue) {
        List<String> allowedOrigins = properties.getAllowedOrigins();
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return true;
        }
        if (allowedOrigins.stream().anyMatch("*"::equals)) {
            return true;
        }
        return originHeaderValue != null && allowedOrigins.contains(originHeaderValue);
    }

    /**
     * 注入 Spring 管理的 Endpoint 实例。
     *
     * @param endpointClass Endpoint 类型
     * @return Endpoint 实例
     * @param <T> Endpoint 泛型
     */
    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) {
        return endpointClass.cast(endpoint);
    }

    /**
     * 在握手阶段复制请求头与 TraceId。
     *
     * @param config Endpoint 配置
     * @param request 握手请求
     * @param response 这里不处理握手响应
     */
    @Override
    public void modifyHandshake(
            ServerEndpointConfig config,
            HandshakeRequest request,
            jakarta.websocket.HandshakeResponse response) {
        config.getUserProperties().put("headers", request.getHeaders());
        String traceId = firstHeaderValue(request.getHeaders(), properties.getTraceHeaderName());
        if (StringUtils.hasText(traceId)) {
            config.getUserProperties().put(WebSocketSessionAttributes.TRACE_ID, traceId.trim());
        }
    }

    /**
     * 读取首个请求头值。
     *
     * @param headers 请求头映射
     * @param headerName 头名称
     * @return 首个头值
     */
    private String firstHeaderValue(Map<String, List<String>> headers, String headerName) {
        if (headers == null || !StringUtils.hasText(headerName)) {
            return null;
        }
        List<String> values = headers.get(headerName);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }
}
