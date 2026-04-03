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
import com.getboot.ai.api.prompt.PromptTemplate;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 默认提示词模板渲染器测试。
 *
 * @author qiheng
 */
class DefaultPromptTemplateRendererTest {

    /**
     * 验证模板变量会被正确替换。
     */
    @Test
    void shouldRenderPromptTemplate() {
        PromptTemplate template = new PromptTemplate();
        template.setContent("Hello {{name}}, order {{orderNo}} is ready.");
        template.setVariables(Map.of("name", "GetBoot", "orderNo", "A1001"));

        String rendered = new DefaultPromptTemplateRenderer().render(template);

        assertEquals("Hello GetBoot, order A1001 is ready.", rendered);
    }

    /**
     * 验证缺少变量时会抛出异常。
     */
    @Test
    void shouldThrowWhenTemplateVariableMissing() {
        PromptTemplate template = new PromptTemplate();
        template.setContent("Hello {{name}}");

        assertThrows(AiException.class, () -> new DefaultPromptTemplateRenderer().render(template));
    }
}
