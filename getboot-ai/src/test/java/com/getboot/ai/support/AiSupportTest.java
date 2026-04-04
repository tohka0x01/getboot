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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AiSupport} 测试。
 *
 * @author qiheng
 */
class AiSupportTest {

    /**
     * 验证文本校验、文本存在判断与模型解析。
     */
    @Test
    void shouldValidateTextAndResolveModel() {
        assertEquals("hello", AiSupport.requireText(" hello ", "message"));
        assertTrue(AiSupport.hasText(" hi "));
        assertFalse(AiSupport.hasText("   "));
        assertEquals("gpt-5-mini", AiSupport.resolveModel(" gpt-5-mini ", "default-model", "chat model"));
        assertEquals("default-model", AiSupport.resolveModel(null, " default-model ", "chat model"));

        assertThrows(AiException.class, () -> AiSupport.requireText("  ", "message"));
        assertThrows(AiException.class, () -> AiSupport.resolveModel("  ", " ", "chat model"));
    }

    /**
     * 验证文本列表与 topK 归一化。
     */
    @Test
    void shouldValidateTextListAndNormalizeTopK() {
        assertEquals(List.of("a", "b"), AiSupport.requireTextList(List.of(" a ", "b"), "Embedding inputs"));
        assertEquals(3, AiSupport.normalizeTopK(null, 3));
        assertEquals(3, AiSupport.normalizeTopK(0, 3));
        assertEquals(2, AiSupport.normalizeTopK(2, 3));
        assertEquals(3, AiSupport.normalizeTopK(5, 3));

        assertThrows(AiException.class, () -> AiSupport.requireTextList(List.of(), "Embedding inputs"));
        assertThrows(AiException.class, () -> AiSupport.requireTextList(List.of("ok", " "), "Embedding inputs"));
        assertThrows(AiException.class, () -> AiSupport.normalizeTopK(1, 0));
    }
}
