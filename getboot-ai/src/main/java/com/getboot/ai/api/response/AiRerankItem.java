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

/**
 * 重排结果项。
 *
 * @author qiheng
 */
@Data
public class AiRerankItem {

    /**
     * 原始文档下标。
     */
    private Integer index;

    /**
     * 原始文档内容。
     */
    private String document;

    /**
     * 相似度分值。
     */
    private Double score;
}
