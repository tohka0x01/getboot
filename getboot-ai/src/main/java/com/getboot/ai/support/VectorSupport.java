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

import java.util.List;

/**
 * 向量辅助工具。
 *
 * @author qiheng
 */
public final class VectorSupport {

    /**
     * 工具类不允许实例化。
     */
    private VectorSupport() {
    }

    /**
     * 计算余弦相似度。
     *
     * @param left 左侧向量
     * @param right 右侧向量
     * @return 余弦相似度
     */
    public static double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            throw new AiException("Embedding vectors must not be empty.");
        }
        if (left.size() != right.size()) {
            throw new AiException("Embedding vector dimensions must match.");
        }

        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int index = 0; index < left.size(); index++) {
            double leftValue = left.get(index);
            double rightValue = right.get(index);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
