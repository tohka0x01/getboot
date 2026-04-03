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
package com.getboot.ai.api.request;

import com.getboot.ai.api.model.AiChatMessage;
import com.getboot.ai.api.prompt.PromptTemplate;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 聊天请求。
 *
 * @author qiheng
 */
@Data
public class AiChatRequest {

    /**
     * 指定模型名。
     */
    private String model;

    /**
     * 开发者或系统指令。
     */
    private String instructions;

    /**
     * 历史消息列表。
     */
    private List<AiChatMessage> messages = new ArrayList<>();

    /**
     * 用户提示词模板。
     */
    private PromptTemplate promptTemplate;

    /**
     * 指定 reasoning effort。
     */
    private String reasoningEffort;

    /**
     * 最大输出 Token 数。
     */
    private Integer maxOutputTokens;

    /**
     * 设置历史消息列表。
     *
     * @param messages 历史消息列表
     */
    public void setMessages(List<AiChatMessage> messages) {
        this.messages = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
    }
}
