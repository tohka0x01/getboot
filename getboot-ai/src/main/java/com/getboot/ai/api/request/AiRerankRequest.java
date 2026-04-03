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
 * AI 重排请求。
 *
 * @author qiheng
 */
@Data
public class AiRerankRequest {

    /**
     * 指定重排使用的向量模型名。
     */
    private String embeddingModel;

    /**
     * 查询文本。
     */
    private String query;

    /**
     * 待重排文档列表。
     */
    private List<String> documents = new ArrayList<>();

    /**
     * 期望返回的前 K 个结果。
     */
    private Integer topK;

    /**
     * 设置待重排文档列表。
     *
     * @param documents 待重排文档列表
     */
    public void setDocuments(List<String> documents) {
        this.documents = documents == null ? new ArrayList<>() : new ArrayList<>(documents);
    }
}
