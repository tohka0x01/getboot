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

import com.getboot.ai.api.constant.AiConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 模块配置。
 *
 * @author qiheng
 */
@ConfigurationProperties(prefix = "getboot.ai")
@Data
public class AiProperties {

    /**
     * 是否启用 AI 能力。
     */
    private boolean enabled = true;

    /**
     * 当前 AI 实现类型。
     */
    private String type = AiConstants.AI_TYPE_OPENAI;

    /**
     * OpenAI 配置。
     */
    private OpenAi openai = new OpenAi();

    /**
     * OpenAI 配置。
     *
     * @author qiheng
     */
    @Data
    public static class OpenAi {

        /**
         * 是否启用 OpenAI 实现。
         */
        private boolean enabled = true;

        /**
         * OpenAI 接口地址。
         */
        private String baseUrl = AiConstants.DEFAULT_OPENAI_BASE_URL;

        /**
         * OpenAI API Key。
         */
        private String apiKey;

        /**
         * 组织标识。
         */
        private String organization;

        /**
         * 项目标识。
         */
        private String project;

        /**
         * 默认聊天模型。
         */
        private String defaultChatModel = AiConstants.DEFAULT_CHAT_MODEL;

        /**
         * 默认向量模型。
         */
        private String defaultEmbeddingModel = AiConstants.DEFAULT_EMBEDDING_MODEL;

        /**
         * 默认 reasoning effort。
         */
        private String defaultReasoningEffort = AiConstants.DEFAULT_REASONING_EFFORT;

        /**
         * 连接超时时间。
         */
        private Duration connectTimeout = AiConstants.DEFAULT_CONNECT_TIMEOUT;

        /**
         * 读取超时时间。
         */
        private Duration readTimeout = AiConstants.DEFAULT_READ_TIMEOUT;

        /**
         * 默认请求头。
         */
        private Map<String, String> defaultHeaders = new LinkedHashMap<>();

        /**
         * 设置默认请求头。
         *
         * @param defaultHeaders 默认请求头
         */
        public void setDefaultHeaders(Map<String, String> defaultHeaders) {
            this.defaultHeaders = defaultHeaders == null ? new LinkedHashMap<>() : new LinkedHashMap<>(defaultHeaders);
        }
    }
}
