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
package com.getboot.ai.spi;

import com.getboot.ai.api.prompt.PromptTemplate;

/**
 * 提示词模板渲染器。
 *
 * @author qiheng
 */
public interface PromptTemplateRenderer {

    /**
     * 渲染提示词模板。
     *
     * @param template 提示词模板
     * @return 渲染后的提示词
     */
    String render(PromptTemplate template);
}
