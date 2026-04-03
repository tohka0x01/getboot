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
import com.getboot.ai.spi.PromptTemplateRenderer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认提示词模板渲染器。
 *
 * @author qiheng
 */
public class DefaultPromptTemplateRenderer implements PromptTemplateRenderer {

    /**
     * 模板占位符模式。
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");

    /**
     * 渲染提示词模板。
     *
     * @param template 提示词模板
     * @return 渲染后的提示词
     */
    @Override
    public String render(PromptTemplate template) {
        if (template == null) {
            throw new AiException("Prompt template must not be null.");
        }
        String content = AiSupport.requireText(template.getContent(), "Prompt template content");
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object variableValue = template.getVariables().get(variableName);
            if (variableValue == null) {
                throw new AiException("Prompt template variable '" + variableName + "' is missing.");
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(variableValue)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
