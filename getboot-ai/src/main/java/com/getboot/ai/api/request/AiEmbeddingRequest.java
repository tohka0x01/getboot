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

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 向量化请求。
 *
 * @author qiheng
 */
@Data
public class AiEmbeddingRequest {

    /**
     * 指定向量模型名。
     */
    private String model;

    /**
     * 待向量化文本列表。
     */
    private List<String> inputs = new ArrayList<>();

    /**
     * 设置待向量化文本列表。
     *
     * @param inputs 待向量化文本列表
     */
    public void setInputs(List<String> inputs) {
        this.inputs = inputs == null ? new ArrayList<>() : new ArrayList<>(inputs);
    }
}
