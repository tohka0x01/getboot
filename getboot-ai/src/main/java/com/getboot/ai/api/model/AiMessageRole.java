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
package com.getboot.ai.api.model;

/**
 * AI 消息角色。
 *
 * @author qiheng
 */
public enum AiMessageRole {

    /**
     * Developer 指令角色。
     */
    DEVELOPER("developer"),

    /**
     * System 指令角色。
     */
    SYSTEM("system"),

    /**
     * User 输入角色。
     */
    USER("user"),

    /**
     * Assistant 输出角色。
     */
    ASSISTANT("assistant");

    /**
     * OpenAI 接口使用的角色值。
     */
    private final String apiValue;

    /**
     * 创建消息角色。
     *
     * @param apiValue 接口角色值
     */
    AiMessageRole(String apiValue) {
        this.apiValue = apiValue;
    }

    /**
     * 返回接口角色值。
     *
     * @return 接口角色值
     */
    public String getApiValue() {
        return apiValue;
    }
}
