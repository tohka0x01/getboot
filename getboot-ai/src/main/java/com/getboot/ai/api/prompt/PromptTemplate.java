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
package com.getboot.ai.api.prompt;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 提示词模板。
 *
 * @author qiheng
 */
@Data
public class PromptTemplate {

    /**
     * 模板内容。
     */
    private String content;

    /**
     * 模板变量。
     */
    private Map<String, Object> variables = new LinkedHashMap<>();

    /**
     * 设置模板变量。
     *
     * @param variables 模板变量
     */
    public void setVariables(Map<String, Object> variables) {
        this.variables = variables == null ? new LinkedHashMap<>() : new LinkedHashMap<>(variables);
    }
}
