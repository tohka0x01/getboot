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
 * AI 向量化响应。
 *
 * @author qiheng
 */
@Data
public class AiEmbeddingResponse {

    /**
     * 实际模型名。
     */
    private String model;

    /**
     * 向量项列表。
     */
    private List<AiEmbeddingItem> items = new ArrayList<>();

    /**
     * 输入 Token 数。
     */
    private Integer promptTokens;

    /**
     * 总 Token 数。
     */
    private Integer totalTokens;

    /**
     * 设置向量项列表。
     *
     * @param items 向量项列表
     */
    public void setItems(List<AiEmbeddingItem> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }
}
