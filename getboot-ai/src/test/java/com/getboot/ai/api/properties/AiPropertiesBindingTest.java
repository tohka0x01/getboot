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
package com.getboot.ai.api.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 配置绑定测试。
 *
 * @author qiheng
 */
class AiPropertiesBindingTest {

    /**
     * 验证 kebab-case 配置能够绑定到 AI 属性。
     */
    @Test
    void shouldBindAiPropertiesFromKebabCaseConfiguration() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("getboot.ai.enabled", "false");
        source.put("getboot.ai.type", "openai");
        source.put("getboot.ai.openai.enabled", "true");
        source.put("getboot.ai.openai.base-url", "https://example.com/v1");
        source.put("getboot.ai.openai.api-key", "sk-test");
        source.put("getboot.ai.openai.default-chat-model", "gpt-5-mini");
        source.put("getboot.ai.openai.default-embedding-model", "text-embedding-3-small");
        source.put("getboot.ai.openai.default-reasoning-effort", "medium");
        source.put("getboot.ai.openai.connect-timeout", "4s");
        source.put("getboot.ai.openai.read-timeout", "40s");
        source.put("getboot.ai.openai.default-headers.X-Request-Source", "getboot");

        AiProperties properties = new Binder(new MapConfigurationPropertySource(source))
                .bind("getboot.ai", Bindable.of(AiProperties.class))
                .orElseThrow(() -> new IllegalStateException("ai properties should bind"));

        assertFalse(properties.isEnabled());
        assertEquals("openai", properties.getType());
        assertTrue(properties.getOpenai().isEnabled());
        assertEquals("https://example.com/v1", properties.getOpenai().getBaseUrl());
        assertEquals("sk-test", properties.getOpenai().getApiKey());
        assertEquals("gpt-5-mini", properties.getOpenai().getDefaultChatModel());
        assertEquals("text-embedding-3-small", properties.getOpenai().getDefaultEmbeddingModel());
        assertEquals("medium", properties.getOpenai().getDefaultReasoningEffort());
        assertEquals(Duration.ofSeconds(4), properties.getOpenai().getConnectTimeout());
        assertEquals(Duration.ofSeconds(40), properties.getOpenai().getReadTimeout());
        assertEquals("getboot", properties.getOpenai().getDefaultHeaders().get("X-Request-Source"));
    }
}
