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
package com.getboot.ai.infrastructure.autoconfigure;

import com.getboot.ai.api.operator.AiOperator;
import com.getboot.ai.infrastructure.openai.support.OpenAiRestGateway;
import com.getboot.ai.spi.AiModelClient;
import com.getboot.ai.spi.PromptTemplateRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 自动配置测试。
 *
 * @author qiheng
 */
class AiAutoConfigurationTest {

    /**
     * 上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AiAutoConfiguration.class))
            .withPropertyValues(
                    "getboot.ai.enabled=true",
                    "getboot.ai.type=openai",
                    "getboot.ai.openai.enabled=true",
                    "getboot.ai.openai.api-key=sk-test"
            );

    /**
     * 验证自动配置会注册默认 AI Bean。
     */
    @Test
    void shouldRegisterDefaultAiBeans() {
        contextRunner.run(context -> {
            assertInstanceOf(PromptTemplateRenderer.class, context.getBean(PromptTemplateRenderer.class));
            assertInstanceOf(HttpClient.class, context.getBean(HttpClient.class));
            assertInstanceOf(OpenAiRestGateway.class, context.getBean(OpenAiRestGateway.class));
            assertInstanceOf(AiModelClient.class, context.getBean(AiModelClient.class));
            assertInstanceOf(AiOperator.class, context.getBean(AiOperator.class));
        });
    }

    /**
     * 验证禁用 AI 能力时跳过全部相关 Bean。
     */
    @Test
    void shouldSkipAllAiBeansWhenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AiAutoConfiguration.class))
                .withPropertyValues(
                        "getboot.ai.enabled=false",
                        "getboot.ai.openai.api-key=sk-test"
                )
                .run(context -> {
                    assertFalse(context.containsBean("promptTemplateRenderer"));
                    assertFalse(context.containsBean("openAiHttpClient"));
                    assertFalse(context.containsBean("openAiRestGateway"));
                    assertFalse(context.containsBean("aiModelClient"));
                    assertFalse(context.containsBean("aiOperator"));
                });
    }

    /**
     * 验证缺少 API Key 时仅保留核心 Bean。
     */
    @Test
    void shouldSkipOpenAiBeansWhenApiKeyMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AiAutoConfiguration.class))
                .withPropertyValues(
                        "getboot.ai.enabled=true",
                        "getboot.ai.type=openai",
                        "getboot.ai.openai.enabled=true"
                )
                .run(context -> {
                    assertTrue(context.containsBean("promptTemplateRenderer"));
                    assertFalse(context.containsBean("openAiHttpClient"));
                    assertFalse(context.containsBean("openAiRestGateway"));
                    assertFalse(context.containsBean("aiModelClient"));
                    assertFalse(context.containsBean("aiOperator"));
                });
    }

    /**
     * 验证切换为非 OpenAI 类型时不会注册 OpenAI 相关 Bean。
     */
    @Test
    void shouldSkipOpenAiBeansWhenTypeIsNotOpenAi() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AiAutoConfiguration.class))
                .withPropertyValues(
                        "getboot.ai.enabled=true",
                        "getboot.ai.type=azure-openai",
                        "getboot.ai.openai.enabled=true",
                        "getboot.ai.openai.api-key=sk-test"
                )
                .run(context -> {
                    assertTrue(context.containsBean("promptTemplateRenderer"));
                    assertFalse(context.containsBean("openAiHttpClient"));
                    assertFalse(context.containsBean("openAiRestGateway"));
                    assertFalse(context.containsBean("aiModelClient"));
                    assertFalse(context.containsBean("aiOperator"));
                });
    }
}
