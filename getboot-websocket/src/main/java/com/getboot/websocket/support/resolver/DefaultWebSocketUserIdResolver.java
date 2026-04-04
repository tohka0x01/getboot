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
package com.getboot.websocket.support.resolver;

import com.getboot.websocket.api.properties.WebSocketProperties;
import com.getboot.websocket.spi.WebSocketUserIdResolver;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 默认 WebSocket 用户标识解析器。
 *
 * @author qiheng
 */
public class DefaultWebSocketUserIdResolver implements WebSocketUserIdResolver {

    /**
     * WebSocket 模块配置。
     */
    private final WebSocketProperties properties;

    /**
     * 创建默认用户标识解析器。
     *
     * @param properties WebSocket 模块配置
     */
    public DefaultWebSocketUserIdResolver(WebSocketProperties properties) {
        this.properties = properties;
    }

    /**
     * 解析当前连接对应的用户标识。
     *
     * @param session WebSocket 会话
     * @param endpointConfig Endpoint 配置
     * @return 用户标识
     */
    @Override
    public String resolve(Session session, EndpointConfig endpointConfig) {
        Map<String, List<String>> requestParameters = session.getRequestParameterMap();
        if (requestParameters != null) {
            String parameterValue = firstValue(requestParameters.get(properties.getUserIdQueryParameter()));
            if (StringUtils.hasText(parameterValue)) {
                return parameterValue.trim();
            }
        }

        Object headers = endpointConfig.getUserProperties().get("headers");
        if (headers instanceof Map<?, ?> headerMap) {
            Object rawValues = headerMap.get(properties.getUserIdHeaderName());
            if (rawValues instanceof List<?> values) {
                String headerValue = firstValue(values);
                if (StringUtils.hasText(headerValue)) {
                    return headerValue.trim();
                }
            }
        }

        Principal principal = session.getUserPrincipal();
        if (principal != null && StringUtils.hasText(principal.getName())) {
            return principal.getName().trim();
        }
        return null;
    }

    /**
     * 读取首个字符串值。
     *
     * @param values 原始值集合
     * @return 首个字符串值
     */
    private String firstValue(List<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        Object first = values.get(0);
        return first == null ? null : first.toString();
    }
}
