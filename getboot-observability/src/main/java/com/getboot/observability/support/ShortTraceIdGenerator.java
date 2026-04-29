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
package com.getboot.observability.support;

import com.getboot.observability.spi.TraceIdGenerator;

import java.security.SecureRandom;

/**
 * 短 TraceId 生成器。
 *
 * <p>生成小写字母和数字组合，适合控制台、日志和响应体快速复制排查。</p>
 *
 * @author qiheng
 */
public class ShortTraceIdGenerator implements TraceIdGenerator {

    /**
     * 最小长度。
     */
    private static final int MIN_LENGTH = 8;

    /**
     * 最大长度。
     */
    private static final int MAX_LENGTH = 32;

    /**
     * 默认长度。
     */
    private static final int DEFAULT_LENGTH = 16;

    /**
     * 可选字符。
     */
    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

    /**
     * 随机数生成器。
     */
    private final SecureRandom random = new SecureRandom();

    /**
     * TraceId 长度。
     */
    private final int length;

    /**
     * 创建短 TraceId 生成器。
     *
     * @param length TraceId 长度
     */
    public ShortTraceIdGenerator(int length) {
        this.length = normalizeLength(length);
    }

    /**
     * 生成短 TraceId。
     *
     * @return 小写字母和数字组合
     */
    @Override
    public String generate() {
        char[] value = new char[length];
        for (int index = 0; index < value.length; index++) {
            value[index] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(value);
    }

    private int normalizeLength(int length) {
        if (length <= 0) {
            return DEFAULT_LENGTH;
        }
        return Math.max(MIN_LENGTH, Math.min(MAX_LENGTH, length));
    }
}
