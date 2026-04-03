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
package com.getboot.ai.api.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 重排响应。
 *
 * @author qiheng
 */
@Data
public class AiRerankResponse {

    /**
     * 实际使用的向量模型。
     */
    private String embeddingModel;

    /**
     * 重排结果列表。
     */
    private List<AiRerankItem> items = new ArrayList<>();

    /**
     * 设置重排结果列表。
     *
     * @param items 重排结果列表
     */
    public void setItems(List<AiRerankItem> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }
}
