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
package com.getboot.ai.infrastructure.openai.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getboot.ai.api.properties.AiProperties;
import com.getboot.ai.infrastructure.openai.support.DefaultOpenAiRestGateway;
import com.getboot.ai.infrastructure.openai.support.OpenAiAiModelClient;
import com.getboot.ai.infrastructure.openai.support.OpenAiRestGateway;
import com.getboot.ai.spi.AiModelClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.net.http.HttpClient;

/**
 * OpenAI 自动配置。
 *
 * @author qiheng
 */
@AutoConfiguration
@ConditionalOnClass(HttpClient.class)
@ConditionalOnProperty(prefix = "getboot.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnExpression("${getboot.ai.openai.enabled:true} and '${getboot.ai.type:openai}' == 'openai' and !'${getboot.ai.openai.api-key:}'.isEmpty()")
public class OpenAiAutoConfiguration {

    /**
     * 注册 OpenAI HTTP 客户端。
     *
     * @param properties AI 模块配置
     * @return HTTP 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public HttpClient openAiHttpClient(AiProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getOpenai().getConnectTimeout())
                .build();
    }

    /**
     * 注册 OpenAI 请求网关。
     *
     * @param httpClient HTTP 客户端
     * @param objectMapperProvider Jackson 映射器提供器
     * @param properties AI 模块配置
     * @return OpenAI 请求网关
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAiRestGateway openAiRestGateway(
            HttpClient httpClient,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            AiProperties properties) {
        return new DefaultOpenAiRestGateway(
                httpClient,
                objectMapperProvider.getIfAvailable(ObjectMapper::new),
                properties
        );
    }

    /**
     * 注册 OpenAI 模型客户端。
     *
     * @param gateway OpenAI 请求网关
     * @param properties AI 模块配置
     * @return 模型客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public AiModelClient aiModelClient(OpenAiRestGateway gateway, AiProperties properties) {
        return new OpenAiAiModelClient(gateway, properties);
    }
}
