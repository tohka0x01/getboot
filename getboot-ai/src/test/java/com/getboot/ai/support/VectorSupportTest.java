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
package com.getboot.ai.support;

import com.getboot.ai.api.exception.AiException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link VectorSupport} 测试。
 *
 * @author qiheng
 */
class VectorSupportTest {

    /**
     * 验证余弦相似度计算和零向量处理。
     */
    @Test
    void shouldCalculateCosineSimilarity() {
        assertEquals(1.0D, VectorSupport.cosineSimilarity(List.of(1D, 0D), List.of(1D, 0D)), 1e-9);
        assertEquals(0.0D, VectorSupport.cosineSimilarity(List.of(1D, 0D), List.of(0D, 1D)), 1e-9);
        assertEquals(0.0D, VectorSupport.cosineSimilarity(List.of(0D, 0D), List.of(1D, 0D)), 1e-9);
    }

    /**
     * 验证空向量和维度不一致时抛出异常。
     */
    @Test
    void shouldRejectInvalidVectors() {
        assertThrows(AiException.class, () -> VectorSupport.cosineSimilarity(null, List.of(1D)));
        assertThrows(AiException.class, () -> VectorSupport.cosineSimilarity(List.of(), List.of(1D)));
        assertThrows(AiException.class, () -> VectorSupport.cosineSimilarity(List.of(1D), List.of(1D, 2D)));
    }
}
