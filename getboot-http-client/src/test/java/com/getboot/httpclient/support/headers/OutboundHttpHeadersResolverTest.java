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
package com.getboot.httpclient.support.headers;

import com.getboot.httpclient.api.model.OutboundHttpClientType;
import com.getboot.httpclient.api.model.OutboundHttpRequestContext;
import com.getboot.httpclient.spi.OutboundHttpHeadersContributor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link OutboundHttpHeadersResolver} 测试。
 *
 * @author qiheng
 */
class OutboundHttpHeadersResolverTest {

    /**
     * 验证解析器会同时生成 Trace 请求头和通用请求头。
     */
    @Test
    void shouldResolveTraceHeaderAndCommonHeaders() {
        OutboundHttpHeadersContributor contributor = (headers, context) ->
                headers.add("X-App-Id", context.getClientType().name());
        OutboundHttpHeadersResolver resolver = new OutboundHttpHeadersResolver(List.of(contributor));

        HttpHeaders headers = resolver.resolve(
                new OutboundHttpRequestContext(OutboundHttpClientType.OPEN_FEIGN, "trace-001", "X-Trace-Id")
        );

        assertEquals(List.of("OPEN_FEIGN"), headers.get("X-App-Id"));
        assertEquals(List.of("trace-001"), headers.get("X-Trace-Id"));
    }

    /**
     * 验证缺少 TraceId 时仍保留通用请求头。
     */
    @Test
    void shouldKeepCommonHeadersWhenTraceIdMissing() {
        OutboundHttpHeadersContributor contributor = (headers, context) ->
                headers.add("X-Tenant-Id", "tenant-001");
        OutboundHttpHeadersResolver resolver = new OutboundHttpHeadersResolver(List.of(contributor));

        HttpHeaders headers = resolver.resolve(
                new OutboundHttpRequestContext(OutboundHttpClientType.REST_TEMPLATE, null, "X-Trace-Id")
        );

        assertEquals(List.of("tenant-001"), headers.get("X-Tenant-Id"));
        assertFalse(headers.containsKey("X-Trace-Id"));
    }
}
