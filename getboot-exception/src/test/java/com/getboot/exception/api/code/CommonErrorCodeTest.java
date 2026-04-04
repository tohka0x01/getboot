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
package com.getboot.exception.api.code;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link CommonErrorCode} 测试。
 *
 * @author qiheng
 */
class CommonErrorCodeTest {

    /**
     * 验证能够按错误码解析枚举项。
     */
    @Test
    void shouldResolveEnumByCode() {
        assertSame(CommonErrorCode.SUCCESS, CommonErrorCode.fromCode(200));
        assertSame(CommonErrorCode.TOO_MANY_REQUESTS, CommonErrorCode.fromCode(429));
        assertNull(CommonErrorCode.fromCode(999));
    }

    /**
     * 验证接口默认方法会拼接错误码字符串。
     */
    @Test
    void shouldBuildCodeStringFromInterfaceDefaultMethod() {
        assertEquals("404:The requested resource was not found.", CommonErrorCode.NOT_FOUND.toCodeString());
    }

    /**
     * 验证空错误码与边界错误码都能得到稳定结果。
     */
    @Test
    void shouldHandleNullAndBoundaryCodes() {
        assertNull(CommonErrorCode.fromCode(null));
        assertSame(CommonErrorCode.UNAUTHORIZED, CommonErrorCode.fromCode(401));
        assertSame(CommonErrorCode.ERROR, CommonErrorCode.fromCode(500));
    }
}
