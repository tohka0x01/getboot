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
package com.getboot.ai.api.constant;

import java.time.Duration;

/**
 * AI 模块常量。
 *
 * @author qiheng
 */
public final class AiConstants {

    /**
     * OpenAI 类型标识。
     */
    public static final String AI_TYPE_OPENAI = "openai";

    /**
     * 默认 OpenAI 接口地址。
     */
    public static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1";

    /**
     * 默认聊天模型。
     */
    public static final String DEFAULT_CHAT_MODEL = "gpt-5-mini";

    /**
     * 默认向量模型。
     */
    public static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small";

    /**
     * 默认 reasoning effort。
     */
    public static final String DEFAULT_REASONING_EFFORT = "low";

    /**
     * 默认连接超时时间。
     */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);

    /**
     * 默认读取超时时间。
     */
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 工具类不允许实例化。
     */
    private AiConstants() {
    }
}
