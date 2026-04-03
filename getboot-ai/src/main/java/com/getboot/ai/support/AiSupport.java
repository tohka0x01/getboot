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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 模块辅助工具。
 *
 * @author qiheng
 */
public final class AiSupport {

    /**
     * 工具类不允许实例化。
     */
    private AiSupport() {
    }

    /**
     * 校验文本非空。
     *
     * @param value 文本值
     * @param fieldName 字段名
     * @return 规整后的文本值
     */
    public static String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new AiException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    /**
     * 判断文本是否有效。
     *
     * @param value 文本值
     * @return 是否有效
     */
    public static boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    /**
     * 归一化模型名。
     *
     * @param requestedModel 请求模型名
     * @param defaultModel 默认模型名
     * @param fieldName 字段名
     * @return 最终模型名
     */
    public static String resolveModel(String requestedModel, String defaultModel, String fieldName) {
        if (hasText(requestedModel)) {
            return requestedModel.trim();
        }
        return requireText(defaultModel, fieldName);
    }

    /**
     * 校验文本列表。
     *
     * @param values 文本列表
     * @param fieldName 字段名
     * @return 规整后的文本列表
     */
    public static List<String> requireTextList(List<String> values, String fieldName) {
        if (CollectionUtils.isEmpty(values)) {
            throw new AiException(fieldName + " must not be empty.");
        }
        List<String> normalizedValues = new ArrayList<>(values.size());
        for (String value : values) {
            normalizedValues.add(requireText(value, fieldName + " item"));
        }
        return normalizedValues;
    }

    /**
     * 归一化 topK。
     *
     * @param topK 请求 topK
     * @param maxSize 最大结果数
     * @return 最终 topK
     */
    public static int normalizeTopK(Integer topK, int maxSize) {
        if (maxSize < 1) {
            throw new AiException("Rerank documents must not be empty.");
        }
        if (topK == null || topK < 1) {
            return maxSize;
        }
        return Math.min(topK, maxSize);
    }
}
