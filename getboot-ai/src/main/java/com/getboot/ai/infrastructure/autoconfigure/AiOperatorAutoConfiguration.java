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
import com.getboot.ai.spi.AiModelClient;
import com.getboot.ai.spi.PromptTemplateRenderer;
import com.getboot.ai.support.DefaultAiOperator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * AI 门面自动配置。
 *
 * @author qiheng
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "getboot.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiOperatorAutoConfiguration {

    /**
     * 注册默认 AI 门面。
     *
     * @param modelClient 模型客户端
     * @param promptTemplateRenderer 提示词模板渲染器
     * @return AI 门面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AiModelClient.class)
    public AiOperator aiOperator(
            AiModelClient modelClient,
            PromptTemplateRenderer promptTemplateRenderer) {
        return new DefaultAiOperator(modelClient, promptTemplateRenderer);
    }
}
