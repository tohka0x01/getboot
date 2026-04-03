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

import com.getboot.ai.api.properties.AiProperties;
import com.getboot.ai.spi.PromptTemplateRenderer;
import com.getboot.ai.support.DefaultPromptTemplateRenderer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * AI 核心自动配置。
 *
 * @author qiheng
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "getboot.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AiProperties.class)
public class AiCoreAutoConfiguration {

    /**
     * 注册默认提示词模板渲染器。
     *
     * @return 提示词模板渲染器
     */
    @Bean
    @ConditionalOnMissingBean
    public PromptTemplateRenderer promptTemplateRenderer() {
        return new DefaultPromptTemplateRenderer();
    }
}
