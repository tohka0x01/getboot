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
package com.getboot.websocket.api.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * WebSocket 配置绑定测试。
 *
 * @author qiheng
 */
class WebSocketPropertiesBindingTest {

    /**
     * 验证 kebab-case 配置能够绑定到 WebSocket 属性。
     */
    @Test
    void shouldBindWebSocketPropertiesFromKebabCaseConfiguration() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("getboot.websocket.enabled", "false");
        source.put("getboot.websocket.endpoint", "/ws/demo");
        source.put("getboot.websocket.allowed-origins[0]", "https://demo.example.com");
        source.put("getboot.websocket.trace-header-name", "Trace-Id");
        source.put("getboot.websocket.user-id-header-name", "X-Demo-User");
        source.put("getboot.websocket.user-id-query-parameter", "uid");
        source.put("getboot.websocket.async-send-timeout-ms", "3000");

        WebSocketProperties properties = new Binder(new MapConfigurationPropertySource(source))
                .bind("getboot.websocket", Bindable.of(WebSocketProperties.class))
                .orElseThrow(() -> new IllegalStateException("websocket properties should bind"));

        assertFalse(properties.isEnabled());
        assertEquals("/ws/demo", properties.getEndpoint());
        assertEquals("https://demo.example.com", properties.getAllowedOrigins().get(0));
        assertEquals("Trace-Id", properties.getTraceHeaderName());
        assertEquals("X-Demo-User", properties.getUserIdHeaderName());
        assertEquals("uid", properties.getUserIdQueryParameter());
        assertEquals(3000L, properties.getAsyncSendTimeoutMs());
    }
}
