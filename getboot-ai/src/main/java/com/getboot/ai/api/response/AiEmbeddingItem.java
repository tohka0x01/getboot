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
 * 向量项。
 *
 * @author qiheng
 */
@Data
public class AiEmbeddingItem {

    /**
     * 结果序号。
     */
    private Integer index;

    /**
     * 向量值。
     */
    private List<Double> vector = new ArrayList<>();

    /**
     * 设置向量值。
     *
     * @param vector 向量值
     */
    public void setVector(List<Double> vector) {
        this.vector = vector == null ? new ArrayList<>() : new ArrayList<>(vector);
    }
}
